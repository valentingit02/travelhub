"""
Prueba funcional y de integracion de punta a punta, contra el sistema levantado con docker compose.

Recorre lo que pide la consigna: REST con OpenAPI, SOAP, gateway, login y privacidad, Saga, pago asincronico,
mails, viaje compartido, IA por REST y por la cola, APIs externas, DLQ, trazas y health checks.

Uso (desde la raiz del repo, sin dependencias externas):
    docker compose up -d --build
    python tests/e2e/prueba_funcional.py

Sale con codigo 1 si falla algun chequeo. Deja datos de prueba (cuentas smoke*@test.com) en la base local.
"""
import base64
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request

GW = os.getenv("GW", "http://localhost:8080")
SOAP_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "soap")
resultados = []


def req(method, url, body=None, headers=None, raw=None):
    h = dict(headers or {})
    data = None
    if raw is not None:
        data = raw.encode()
    elif body is not None:
        data = json.dumps(body).encode()
        h.setdefault("Content-Type", "application/json")
    r = urllib.request.Request(url, data=data, method=method, headers=h)
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            txt = resp.read().decode("utf-8", "replace")
            return resp.status, txt, dict(resp.headers)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace"), dict(e.headers)
    except Exception as e:  # conexion caida, timeout
        return 0, str(e), {}


def js(txt):
    try:
        return json.loads(txt)
    except Exception:
        return None


def check(area, nombre, ok, detalle=""):
    resultados.append((area, nombre, bool(ok), detalle))
    print(f"{'OK  ' if ok else 'FALLA'} [{area}] {nombre}" + (f" -> {detalle}" if detalle else ""), flush=True)


def esperar(fn, segundos=25, cada=1.0):
    fin = time.time() + segundos
    while time.time() < fin:
        v = fn()
        if v:
            return v
        time.sleep(cada)
    return None


# ------------------------------------------------------------------ REST documentado (OpenAPI)
for nombre, url in [("catalogo", "http://localhost:8081/v3/api-docs"), ("precios", "http://localhost:8082/v3/api-docs"),
                    ("reservas", "http://localhost:8083/v3/api-docs"), ("pagos", "http://localhost:8084/v3/api-docs"),
                    ("notificaciones", "http://localhost:8085/v3/api-docs"), ("ia", "http://localhost:8000/openapi.json")]:
    c, t, _ = req("GET", url)
    d = js(t) or {}
    check("REST/OpenAPI", f"contrato {nombre}", c == 200 and d.get("paths"), f"{len(d.get('paths', {}))} rutas")

# ------------------------------------------------------------------ SOAP
c, t, _ = req("GET", "http://localhost:8082/ws/cotizacion.wsdl")
check("SOAP", "WSDL publicado", c == 200 and "wsdl:definitions" in t and "CotizarPaquete" in t)
for archivo in ["cotizar-producto.xml", "cotizar-paquete.xml"]:
    xml = open(os.path.join(SOAP_DIR, archivo), encoding="utf-8").read()
    c, t, _ = req("POST", "http://localhost:8082/ws", raw=xml, headers={"Content-Type": "text/xml"})
    check("SOAP", f"llamada {archivo}", c == 200 and "Response" in t and "Fault" not in t, f"HTTP {c}")
xml = open(os.path.join(SOAP_DIR, "cotizar-producto.xml"), encoding="utf-8").read()
xml_malo = xml.replace("<c:precioBase>100</c:precioBase>", "<c:precioBase>-5</c:precioBase>")
c, t, _ = req("POST", "http://localhost:8082/ws", raw=xml_malo, headers={"Content-Type": "text/xml"})
check("SOAP", "dato invalido devuelve SOAP Fault (Client)", "Fault" in t and "Client" in t, f"HTTP {c}")

# ------------------------------------------------------------------ Busqueda, cotizacion, gateway
c, t, h = req("GET", f"{GW}/api/catalogo/buscar?origen=AEP&destino=BRC&desde=2027-07-10&hasta=2027-07-15&pax=2")
busq = js(t) or {}
check("Gateway/REST", "buscar paquete (catalogo)", c == 200 and busq.get("hoteles") and busq.get("vuelos"),
      f"{len(busq.get('vuelos', []))} vuelos, {len(busq.get('hoteles', []))} hoteles")
check("Logging", "X-Correlation-Id en la respuesta", any(k.lower() == "x-correlation-id" for k in h))
c, t, _ = req("POST", f"{GW}/api/precios/cotizar", {"tipoProducto": "HOTEL", "precioBase": 100, "moneda": "USD",
              "fechaInicio": "2027-07-10", "fechaFin": "2027-07-13", "pasajeros": 2, "ocupacion": 0.6})
julio = (js(t) or {}).get("precioFinal") or (js(t) or {}).get("total")
c2, t2, _ = req("POST", f"{GW}/api/precios/cotizar", {"tipoProducto": "HOTEL", "precioBase": 100, "moneda": "USD",
                "fechaInicio": "2027-04-10", "fechaFin": "2027-04-13", "pasajeros": 2, "ocupacion": 0.6})
abril = (js(t2) or {}).get("precioFinal") or (js(t2) or {}).get("total")
check("Patrones", "Strategy: julio mas caro que abril", c == 200 and c2 == 200 and julio and abril and julio > abril,
      f"julio={julio} abril={abril}")
c, t, _ = req("GET", f"{GW}/api/catalogo/buscar?origen=AEP&destino=BRC&desde=2027-07-15&hasta=2027-07-10&pax=2")
d = js(t) or {}
check("Errores", "fechas invertidas -> 400 con formato ApiError", c == 400 and "mensaje" in d, d.get("mensaje", ""))

# ------------------------------------------------------------------ Login + reserva + cola + pago + mail
n = random.randint(1000, 99999)
email = f"smoke{n}@test.com"
c, t, _ = req("POST", f"{GW}/api/auth/registro", {"nombre": "Smoke", "apellido": "Test", "email": email,
              "password": "secreta123", "documento": "30111222", "fechaNacimiento": "1995-05-10",
              "preferencias": "aventura"})
tok = (js(t) or {}).get("token")
auth = {"Authorization": f"Bearer {tok}"}
check("Seguridad", "registro con contrasena", c == 201 and tok)

hotel = next(p["id"] for p in busq["hoteles"] if "FALLA" not in p["id"])
vuelo = busq["vuelos"][0]["id"]
c, t, _ = req("POST", f"{GW}/api/reservas", {"destino": "BRC", "desde": "2027-07-10", "hasta": "2027-07-15",
              "pasajeros": 2, "items": [{"tipo": "VUELO", "productoId": vuelo}, {"tipo": "HOTEL", "productoId": hotel}]},
              headers=auth)
r = js(t) or {}
check("Saga/Facade", "reserva creada (PENDIENTE)", c == 201 and r.get("estado") == "PENDIENTE", f"#{r.get('id')}")
rid = r.get("id")


def estado(i):
    return (js(req("GET", f"{GW}/api/reservas/{i}", headers=auth)[1]) or {}).get("estado")


fin = esperar(lambda: estado(rid) if estado(rid) != "PENDIENTE" else None)
check("Mensajeria", "pago asincronico por RabbitMQ -> CONFIRMADA", fin == "CONFIRMADA", str(fin))


def mails_para(dest):
    d = js(req("GET", f"http://localhost:8025/api/v1/search?query=to:{dest}")[1]) or {}
    return d.get("messages") or []


m = esperar(lambda: mails_para(email), 30)
check("Mensajeria", "mail de confirmacion (consumidor notificaciones)", m, m[0]["Subject"] if m else "sin mail")

# Saga con compensacion
c, t, _ = req("POST", f"{GW}/api/reservas", {"destino": "BRC", "desde": "2027-08-10", "hasta": "2027-08-12",
              "pasajeros": 1, "items": [{"tipo": "VUELO", "productoId": busq["vuelos"][1]["id"]},
                                        {"tipo": "HOTEL", "productoId": "MOCK-HOTEL-BRC-FALLA"}]}, headers=auth)
r2 = js(t) or {}
pasos = [p["paso"] for p in r2.get("saga", [])]
check("Saga", "hotel falla -> compensa el vuelo", r2.get("estado") == "FALLIDA" and any("COMPENSAR" in p for p in pasos),
      f"{r2.get('estado')} {pasos}" if r2 else f"HTTP {c} {t[:120]}")

# Viaje compartido
amigo = f"amigo{n}@test.com"
c, t, _ = req("POST", f"{GW}/api/reservas", {"destino": "BRC", "desde": "2027-09-10", "hasta": "2027-09-13",
              "pasajeros": 2, "items": [{"tipo": "HOTEL", "productoId": hotel}], "compartirCon": [amigo]}, headers=auth)
r3 = js(t) or {}
part = [p for p in r3.get("participantes", []) if not p["organizador"]]
check("Viaje compartido", "reserva compartida con invitado", c == 201 and r3.get("compartida") and part)
if part:
    inv = esperar(lambda: mails_para(amigo), 30)
    check("Viaje compartido", "mail de invitacion al amigo", inv)
    c, t, _ = req("POST", f"{GW}/api/reservas/compartidas/{part[0]['token']}/pagar", {"nombre": "Amigo"})
    check("Viaje compartido", "el amigo paga sin cuenta (link publico)", c == 200, f"HTTP {c}")

# ------------------------------------------------------------------ Seguridad y privacidad
check("Seguridad", "reservas sin token -> 401", req("GET", f"{GW}/api/reservas")[0] == 401)
check("Seguridad", "token adulterado -> 401",
      req("GET", f"{GW}/api/reservas", headers={"Authorization": f"Bearer {tok[:-4]}AAAA"})[0] == 401)
c, t, _ = req("POST", f"{GW}/api/auth/login", {"email": email, "password": "otra-clave"})
check("Seguridad", "contrasena incorrecta -> 401", c == 401, (js(t) or {}).get("mensaje", ""))
otro = f"otro{n}@test.com"
c, t, _ = req("POST", f"{GW}/api/auth/registro", {"nombre": "Otro", "apellido": "Usuario", "email": otro,
              "password": "secreta123", "documento": "30999888", "fechaNacimiento": "1990-01-01"})
auth2 = {"Authorization": f"Bearer {(js(t) or {}).get('token')}"}
check("Seguridad", "ver la reserva de otro -> 403", req("GET", f"{GW}/api/reservas/{rid}", headers=auth2)[0] == 403)
check("Seguridad", "cancelar la reserva de otro -> 403", req("DELETE", f"{GW}/api/reservas/{rid}", headers=auth2)[0] == 403)
check("Seguridad", "listar reservas de otro -> 403",
      req("GET", f"{GW}/api/reservas?viajeroId={r.get('viajeroId')}", headers=auth2)[0] == 403)
c, t, _ = req("GET", f"{GW}/api/viajeros/me", headers=auth)
yo = js(t) or {}
check("Privacidad", "perfil sin hash y con documento enmascarado",
      c == 200 and "passwordHash" not in yo and yo.get("documentoEnmascarado", "").startswith("****"),
      yo.get("documentoEnmascarado", ""))

# ------------------------------------------------------------------ IA
c, t, _ = req("GET", f"{GW}/api/ia/recomendaciones?destino=BRC&desde=2027-07-10&hasta=2027-07-15&preferencias=aventura")
d = js(t) or {}
check("IA", "recomendaciones por REST", c == 200 and d.get("recomendaciones"), d.get("recomendaciones", [{}])[0].get("nombre", ""))
c, t, _ = req("POST", f"{GW}/api/ia/asistente", {"mensaje": "Quiero ir a la nieve en julio con mi pareja", "contexto": None})
d = js(t) or {}
check("IA", "asistente conversacional", c == 200 and d.get("respuesta"), (d.get("respuesta") or "")[:70])
c, t, _ = req("GET", f"{GW}/api/ia/kit?destino=BRC&desde=2027-07-10&hasta=2027-07-14&pax=2&preferencias=aventura")
d = js(t) or {}
check("API externa", "Open-Meteo (clima real en el kit)", c == 200 and d.get("clima"), str(d.get("clima", {}).get("fuente") if d.get("clima") else None))
check("API externa", "Open Library (libros)", d.get("libros"), d.get("libros", [{}])[0].get("fuente", "") if d.get("libros") else "")
check("IA", "kit con mapa de excursiones", d.get("mapa") and any("lat" in x for x in d.get("itinerario", [])))
c, t, _ = req("GET", f"{GW}/api/ia/anomalias/modelo")
check("IA", "modelo de anomalias entrenado", c == 200, t[:90])

# ------------------------------------------------------------------ Rol ADMIN (operaciones de operador)
# Cuenta de operador: su email tiene que estar en ADMIN_EMAILS (docker-compose usa admin@travelhub.local por defecto)
ADMIN_EMAIL = os.getenv("ADMIN_EMAIL", "admin@travelhub.local")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD", "admin-demo-123")
c, t, _ = req("POST", f"{GW}/api/auth/login", {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD})
if c != 200:   # primera vez: se crea la cuenta del operador
    c, t, _ = req("POST", f"{GW}/api/auth/registro", {"nombre": "Operador", "apellido": "TravelHub", "email": ADMIN_EMAIL,
                  "password": ADMIN_PASSWORD, "documento": "20000000", "fechaNacimiento": "1990-01-01"})
sesion_admin = js(t) or {}
admin = {"Authorization": f"Bearer {sesion_admin.get('token')}"}
check("Seguridad", "el operador tiene el rol ADMIN", "ADMIN" in (sesion_admin.get("roles") or []),
      f"{ADMIN_EMAIL} -> {sesion_admin.get('roles')} (debe estar en ADMIN_EMAILS)")

AUTO = f"{GW}/api/catalogo/autos/2"
c, t, _ = req("GET", AUTO)
auto = js(t) or {}
check("Seguridad", "ver autos sigue siendo publico", c == 200 and auto)
if auto:
    original = dict(auto)
    cuerpo = {k: auto[k] for k in ["destinoIata", "categoria", "marca", "modelo", "plazas", "transmisionAutomatica", "moneda"]}
    check("Seguridad", "cambiar un precio sin token -> 401", req("PUT", AUTO, {**cuerpo, "precioBaseDia": 1})[0] == 401)
    check("Seguridad", "cambiar un precio siendo viajero -> 403",
          req("PUT", AUTO, {**cuerpo, "precioBaseDia": 1}, headers=auth)[0] == 403)
    check("Seguridad", "aprobar productos siendo viajero -> 403",
          req("POST", f"{AUTO}/aprobar", headers=auth)[0] == 403)

    # Anomalia por la cola: precio.cambiado -> ia -> anomalia.detectada -> catalogo pone el auto en revision
    c, _, _ = req("PUT", AUTO, {**cuerpo, "precioBaseDia": 1}, headers=admin)
    check("Seguridad", "el operador cambia el precio (ADMIN)", c == 200, f"HTTP {c}")
    rev = esperar(lambda: (js(req("GET", AUTO)[1]) or {}).get("enRevision"), 30)
    check("IA (async)", "auto a 1 USD -> la IA lo marca por la cola", rev, "enRevision=True" if rev else "no se marco")
    # El operador corrige el precio y aprueba el auto: queda como estaba
    req("PUT", AUTO, {**cuerpo, "precioBaseDia": original["precioBaseDia"]}, headers=admin)
    time.sleep(2)
    c, t, _ = req("POST", f"{AUTO}/aprobar", headers=admin)
    check("Seguridad", "el operador aprueba el auto y vuelve a publicarse",
          c == 200 and not (js(t) or {}).get("enRevision"), f"HTTP {c}")

c, _, _ = req("POST", f"{GW}/api/catalogo/reservas-proveedor", {"tipo": "HOTEL", "productoId": "x"})
check("Seguridad", "reservar en el proveedor desde afuera -> bloqueado por el gateway", c == 403, f"HTTP {c}")

# ------------------------------------------------------------------ Colas, DLQ y trazas
cred = base64.b64encode(b"guest:guest").decode()
c, t, _ = req("GET", "http://localhost:15672/api/queues", headers={"Authorization": f"Basic {cred}"})
colas = js(t) or []
nombres = [q["name"] for q in colas]
sin_consumidor = [q["name"] for q in colas if q.get("consumers", 0) == 0 and "dlq" not in q["name"]]
check("Mensajeria", "colas declaradas con consumidores", c == 200 and colas and not sin_consumidor,
      f"{len(colas)} colas" + (f", sin consumidor: {sin_consumidor}" if sin_consumidor else ""))
check("Mensajeria", "dead letter queue", any("dlq" in x for x in nombres))
dlq = [q for q in colas if "dlq" in q["name"]]
if dlq:
    # Informativo: los mensajes que fallaron 3 veces quedan guardados para revisarlos (no es un error del sistema)
    print(f"INFO [Mensajeria] mensajes en travelhub.dlq: {dlq[0].get('messages', 0)}")
c, t, _ = req("GET", "http://localhost:9411/api/v2/services")
check("Observabilidad", "trazas distribuidas en Zipkin", c == 200 and len(js(t) or []) >= 3, t[:100])
for s, p in [("catalogo", 8081), ("precios", 8082), ("reservas", 8083), ("pagos", 8084), ("notificaciones", 8085)]:
    c, t, _ = req("GET", f"http://localhost:{p}/actuator/health")
    check("Salud", f"health {s}", c == 200 and "UP" in t)
c, t, _ = req("GET", "http://localhost:8000/health")
check("Salud", "health ia", c == 200)

ok = sum(1 for r in resultados if r[2])
print(f"\nTOTAL: {ok}/{len(resultados)} OK")
sys.exit(0 if ok == len(resultados) else 1)
