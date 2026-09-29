"""
Asistente de viajes (estilo "Sofia" de Despegar, version simple).

1. Interpreta el mensaje en espanol con reglas (rapido y sin costo). Si no detecta el destino
   y hay un LLM local disponible, le pide ayuda a Ollama (respuesta en JSON).
2. Mantiene el contexto: el cliente reenvia la ultima "intencion" y el mensaje nuevo la completa.
3. Con destino y fechas, busca en el catalogo real y cotiza en precios-service: arma un paquete
   sugerido. La IA SUGIERE; el usuario confirma y el servidor verifica el precio al reservar.
"""
import json
import re
import unicodedata
from datetime import date, timedelta
from typing import Optional

import httpx

from . import recomendaciones
from .clima import clima as consultar_clima
from .config import (CATALOGO_URL, DESTINOS, DESTINOS_VENTA, LLM_TIMEOUT_S, OLLAMA_MODELO, OLLAMA_URL,
                     PRECIOS_URL, SERVICIOS_TIMEOUT_S)

MESES = {"enero": 1, "febrero": 2, "marzo": 3, "abril": 4, "mayo": 5, "junio": 6, "julio": 7, "agosto": 8,
         "septiembre": 9, "setiembre": 9, "octubre": 10, "noviembre": 11, "diciembre": 12}
NUMEROS = {"un": 1, "uno": 1, "una": 1, "dos": 2, "tres": 3, "cuatro": 4, "cinco": 5, "seis": 6, "siete": 7,
           "ocho": 8, "nueve": 9, "diez": 10, "once": 11, "doce": 12, "catorce": 14, "quince": 15}
NUM = r"(\d{1,2}|" + "|".join(NUMEROS) + r")"

PALABRAS_DESTINO = [
    ("BRC", ["bariloche", "nieve", "esqui", "esquiar", "chocolate", "cerro catedral", "llao llao"]),
    ("MDZ", ["mendoza", "vino", "vinos", "bodega", "bodegas", "aconcagua", "vendimia"]),
    ("IGR", ["iguazu", "cataratas", "selva", "misiones"]),
    ("USH", ["ushuaia", "fin del mundo", "tierra del fuego", "pinguino", "pinguinos", "canal beagle"]),
    ("MAD", ["madrid", "espana", "europa", "tapas", "prado"]),
]
SIN_OFERTA = ["playa", "mar", "surf", "caribe", "brasil", "cancun", "punta cana", "mar del plata"]
INTERESES = {
    "aventura": ["aventura", "trekking", "rafting", "esqui", "esquiar", "adrenalina", "escalar", "kayak"],
    "naturaleza": ["naturaleza", "paisaje", "paisajes", "lago", "lagos", "montana", "tranquilo", "tranquila",
                   "relajar", "descansar", "cataratas"],
    "gastronomia": ["comer", "gastronomia", "vino", "vinos", "bodega", "tapas", "chocolate", "restaurante"],
    "cultura": ["museo", "museos", "cultura", "historia", "arte", "teatro"],
}
ORIGENES = {"cordoba": "COR", "rosario": "ROS", "ezeiza": "EZE"}


def _normalizar(texto: str) -> str:
    t = unicodedata.normalize("NFD", texto.lower())
    return "".join(c for c in t if unicodedata.category(c) != "Mn")


def _num(tok: str) -> int:
    return int(tok) if tok.isdigit() else NUMEROS[tok]


def _tiene(t: str, palabra: str) -> bool:
    return re.search(r"\b" + re.escape(palabra) + r"\b", t) is not None


def _proxima(mes: int, dia: int, hoy: date) -> date:
    for anio in (hoy.year, hoy.year + 1):
        try:
            f = date(anio, mes, dia)
        except ValueError:
            f = date(anio, mes, 28)
        if f > hoy + timedelta(days=1):
            return f
    return date(hoy.year + 1, mes, min(dia, 28))


def interpretar(mensaje: str, hoy: Optional[date] = None) -> dict:
    """Extrae destino, fechas, viajeros, intereses, auto y presupuesto de un mensaje en espanol."""
    hoy = hoy or date.today()
    t = _normalizar(mensaje)
    r: dict = {}

    for iata, palabras in PALABRAS_DESTINO:
        if any(_tiene(t, p) for p in palabras):
            r["destino"] = iata
            break
    if "destino" not in r and any(_tiene(t, p) for p in SIN_OFERTA):
        r["sinOferta"] = True

    for nombre, iata in ORIGENES.items():
        if _tiene(t, nombre):
            r["origen"] = iata

    # Duracion
    noches = None
    m = re.search(NUM + r"\s+(dias|noches)", t)
    if m:
        noches = _num(m.group(1)) - (1 if m.group(2) == "dias" else 0)
    elif "dos semanas" in t or "quince dias" in t:
        noches = 14
    elif "una semana" in t or re.search(r"\bsemana\b", t) and "fin de semana" not in t:
        noches = 7
    elif "fin de semana" in t or _tiene(t, "finde"):
        noches = 2
    if noches is not None:
        r["noches"] = max(1, min(noches, 21))

    # Fechas: "del 10 al 15 de julio", "el 10 de julio", "en julio", "fin de semana"
    meses = "|".join(MESES)
    m = re.search(r"del?\s+(\d{1,2})\s+al\s+(\d{1,2})\s+de\s+(" + meses + r")", t)
    if m:
        desde = _proxima(MESES[m.group(3)], int(m.group(1)), hoy)
        hasta_dia = int(m.group(2))
        try:
            hasta = desde.replace(day=hasta_dia)
        except ValueError:
            hasta = desde + timedelta(days=5)
        if hasta <= desde:
            hasta = desde + timedelta(days=max(1, hasta_dia))
        r["desde"], r["hasta"] = desde.isoformat(), hasta.isoformat()
    else:
        m = re.search(r"(\d{1,2})\s+de\s+(" + meses + r")", t)
        m2 = re.search(r"\b(" + meses + r")\b", t)
        desde = None
        if m:
            desde = _proxima(MESES[m.group(2)], int(m.group(1)), hoy)
        elif m2:
            desde = _proxima(MESES[m2.group(1)], 10, hoy)
        elif r.get("noches") == 2:   # fin de semana sin mes: el proximo viernes (al menos 7 dias adelante)
            base = hoy + timedelta(days=7)
            desde = base + timedelta(days=(4 - base.weekday()) % 7)
        elif "proximo mes" in t or "mes que viene" in t:
            desde = hoy + timedelta(days=30)
        if desde:
            r["desde"] = desde.isoformat()
            r["hasta"] = (desde + timedelta(days=r.get("noches", 5))).isoformat()

    # Viajeros
    pax = None
    if m := re.search(r"somos\s+" + NUM, t):
        pax = _num(m.group(1))
    elif m := re.search(NUM + r"\s+(personas|adultos|viajeros|pasajeros)", t):
        pax = _num(m.group(1))
    elif m := re.search(r"con\s+" + NUM + r"\s+amig", t):
        pax = _num(m.group(1)) + 1
    elif re.search(r"con mi (novia|novio|pareja|esposa|esposo|marido|mujer|senora)", t):
        pax = 2
    elif "con mi familia" in t or "en familia" in t:
        pax = 4
    elif re.search(r"\b(solo|sola)\b", t):
        pax = 1
    if pax:
        r["pax"] = max(1, min(pax, 9))

    intereses = [k for k, palabras in INTERESES.items() if any(_tiene(t, p) for p in palabras)]
    if intereses:
        r["intereses"] = intereses
    if re.search(r"\b(auto|autos|manejar|alquilar un auto|road trip)\b", t):
        r["auto"] = True
    m = re.search(r"(?:presupuesto|hasta|menos de|maximo|no mas de)\s*(?:de\s*)?(?:usd|u\$s|us\$|\$)?\s*(\d[\d\.]*)", t) \
        or re.search(r"(\d[\d\.]*)\s*(?:usd|dolares|u\$s)", t)
    if m:
        valor = int(m.group(1).replace(".", ""))
        if valor >= 100:
            r["presupuesto"] = valor
    if re.search(r"\b(lujo|premium|5 estrellas|cinco estrellas)\b", t):
        r["lujo"] = True
    if re.search(r"\b(economico|economica|barato|barata|mas barato)\b", t):
        r["economico"] = True
        r["lujo"] = False
    return r


def _interpretar_llm(mensaje: str) -> dict:
    """Plan B cuando las reglas no encuentran destino: el LLM local elige uno de los disponibles."""
    opciones = ", ".join(f"{k}={DESTINOS[k][0]}" for k in DESTINOS_VENTA)
    prompt = ("Sos un asistente de viajes. Del mensaje del usuario, elegi el destino mas adecuado SOLO entre: "
              f"{opciones}. Si ninguno encaja, usa null. Responde SOLO JSON con la forma "
              '{"destino": "BRC" o null}. Mensaje: ' + mensaje)
    try:
        r = httpx.post(f"{OLLAMA_URL}/api/generate", json={"model": OLLAMA_MODELO, "prompt": prompt, "stream": False,
                                                           "format": "json", "options": {"temperature": 0}},
                       timeout=LLM_TIMEOUT_S)
        r.raise_for_status()
        destino = json.loads(r.json().get("response", "{}")).get("destino")
        return {"destino": destino} if destino in DESTINOS_VENTA else {}
    except Exception:
        return {}


def combinar(anterior: Optional[dict], nuevo: dict) -> dict:
    base = dict(anterior or {})
    base.pop("sinOferta", None)
    for k, v in nuevo.items():
        if k == "intereses":
            base["intereses"] = sorted(set(base.get("intereses", [])) | set(v))
        else:
            base[k] = v
    if "noches" in nuevo and "desde" in base and "hasta" not in nuevo:
        base["hasta"] = (date.fromisoformat(base["desde"]) + timedelta(days=nuevo["noches"])).isoformat()
    return base


# ---------------------------------------------------------------- servicios reales

def buscar_catalogo(i: dict) -> dict:
    r = httpx.get(f"{CATALOGO_URL}/api/catalogo/buscar",
                  params={"origen": i.get("origen", "AEP"), "destino": i["destino"], "desde": i["desde"],
                          "hasta": i["hasta"], "pax": i.get("pax", 2)}, timeout=SERVICIOS_TIMEOUT_S)
    r.raise_for_status()
    return r.json()


def cotizar(productos: list, i: dict) -> dict:
    items = [{"tipoProducto": p["tipo"], "precioBase": p["precioBase"], "moneda": p["moneda"],
              "fechaInicio": i["desde"], "fechaFin": i["hasta"], "pasajeros": i.get("pax", 2),
              "ocupacion": p.get("ocupacion", 0)} for p in productos]
    r = httpx.post(f"{PRECIOS_URL}/api/precios/cotizar-paquete", json={"items": items}, timeout=SERVICIOS_TIMEOUT_S)
    r.raise_for_status()
    return r.json()


def _elegir(resultados: dict, i: dict, clima: Optional[dict], economico: bool = False) -> list:
    elegidos = []
    vuelos = sorted(resultados.get("vuelos", []), key=lambda p: p["precioBase"])
    if vuelos:
        elegidos.append(vuelos[0])
    hoteles = sorted([h for h in resultados.get("hoteles", []) if not h["id"].endswith("FALLA")],
                     key=lambda p: p["precioBase"])
    if hoteles:
        elegidos.append(hoteles[-1] if i.get("lujo") and not economico else hoteles[0])
    if i.get("auto") and not economico:
        autos = sorted(resultados.get("autos", []), key=lambda p: p["precioBase"])
        if autos:
            elegidos.append(autos[0])
    excursiones = resultados.get("excursiones", [])
    if excursiones and not economico:
        orden = [r["nombre"] for r in recomendaciones.recomendar(i["destino"], i.get("intereses", []), clima, top=10)]
        intereses = set(i.get("intereses", []))

        def puntaje(e):
            cat = (e.get("detalle") or {}).get("categoria", "")
            pos = orden.index(e["nombre"]) if e["nombre"] in orden else len(orden)
            return (0 if cat in intereses else 1, pos, e["precioBase"])
        elegidos.append(sorted(excursiones, key=puntaje)[0])
    return elegidos


def _fecha_linda(iso: str) -> str:
    f = date.fromisoformat(iso)
    meses = [k for k in MESES if k != "setiembre"]
    return f"{f.day} de {meses[f.month - 1]}"


def responder(mensaje: str, contexto: Optional[dict] = None, hoy: Optional[date] = None) -> dict:
    nuevo = interpretar(mensaje, hoy)
    generado = "reglas"
    if "destino" not in nuevo and not (contexto or {}).get("destino") and not nuevo.get("sinOferta"):
        extra = _interpretar_llm(mensaje)
        if extra:
            nuevo.update(extra)
            generado = f"reglas+llm:{OLLAMA_MODELO}"
    i = combinar(contexto, nuevo)
    destinos_chips = [f"{DESTINOS[k][0]}" for k in DESTINOS_VENTA]

    if nuevo.get("sinOferta") and "destino" not in i:
        return _resp("Por ahora no vendemos destinos de playa. ¿Te tienta la montaña, las cataratas o una "
                     "escapada a Madrid?", i, ["destino"], destinos_chips, None, generado)
    if "destino" not in i:
        return _resp("¡Dale! ¿A dónde te gustaría ir? Contame qué tenés ganas de hacer (nieve, vinos, "
                     "naturaleza, ciudad) y lo armamos.", i, ["destino"], destinos_chips, None, generado)
    ciudad = DESTINOS[i["destino"]][0]
    if "desde" not in i:
        return _resp(f"{ciudad} es una gran elección. ¿Para cuándo lo pensás y por cuántos días?", i, ["fechas"],
                     ["En julio, 5 noches", "Un fin de semana", "En noviembre, una semana"], None, generado)
    supuesto = ""
    if "pax" not in i:
        i["pax"] = 2
        supuesto = " (asumí 2 viajeros; decime si son más)"

    try:
        resultados = buscar_catalogo(i)
        c = consultar_clima(i["destino"], date.fromisoformat(i["desde"]), date.fromisoformat(i["hasta"]))
        productos = _elegir(resultados, i, c, economico=bool(i.get("economico")))
        if not productos:
            return _resp(f"No encontré disponibilidad en {ciudad} para esas fechas. ¿Probamos otras?", i, ["fechas"],
                         ["Una semana después", "El mes que viene"], None, generado)
        cot = cotizar(productos, i)
        nota_presupuesto = ""
        if i.get("presupuesto") and float(cot["total"]) > i["presupuesto"]:
            baratos = _elegir(resultados, i, c, economico=True)
            cot_b = cotizar(baratos, i)
            if float(cot_b["total"]) < float(cot["total"]):
                productos, cot = baratos, cot_b
            nota_presupuesto = (" Te lo dejé en la versión más económica" if float(cot["total"]) <= i["presupuesto"]
                                else f" Ojo: aun lo más económico supera tu presupuesto de USD {i['presupuesto']}") + "."
    except Exception:
        return _resp("No pude consultar la disponibilidad en este momento. Probá de nuevo en unos segundos.", i, [],
                     [], None, generado)

    temporada = any(f["estrategia"] == "TEMPORADA" and float(f["factor"]) > 1
                    for item in cot["items"] for f in item.get("factores", []))
    partes = ", ".join(p["nombre"] for p in productos)
    texto = (f"Te armé un viaje a {ciudad} del {_fecha_linda(i['desde'])} al {_fecha_linda(i['hasta'])} para "
             f"{i['pax']}{supuesto}: {partes}. Total USD {float(cot['total']):,.0f}".replace(",", ".")
             + (f" ({cot['motivoDescuento'].lower()})" if float(cot["factorPaquete"]) < 1 else "") + "."
             + (" Es temporada alta: si podés mover las fechas, sale más barato." if temporada else "")
             + nota_presupuesto)
    if c:
        texto += f" Se esperan entre {c['min']}° y {c['max']}°."
    sugerencia = {"productos": productos, "subtotal": cot["subtotal"], "total": cot["total"],
                  "moneda": cot["moneda"], "motivoDescuento": cot["motivoDescuento"],
                  "busqueda": {"origen": i.get("origen", "AEP"), "destino": i["destino"], "desde": i["desde"],
                               "hasta": i["hasta"], "pax": i["pax"]}}
    chips = ["Más económico", "Agregá un auto", "Somos 4"]
    return _resp(texto, i, [], chips, sugerencia, generado)


def _resp(texto, intencion, faltantes, opciones, sugerencia, generado):
    return {"respuesta": texto, "intencion": intencion, "faltantes": faltantes, "opciones": opciones,
            "sugerencia": sugerencia, "generadoPor": generado}
