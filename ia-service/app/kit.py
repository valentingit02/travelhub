"""Kit de viaje: que llevar (segun clima y destino), itinerario dia por dia y libros para el viaje."""
from datetime import date, timedelta
from typing import List, Optional

from . import libros, recomendaciones
from .clima import clima as consultar_clima
from .config import AEROPUERTOS, DESTINOS
from .recomendaciones import CATEGORIAS


def equipaje(destino: str, c: Optional[dict], noches: int, preferencias: List[str]) -> List[dict]:
    d = destino.upper()
    items = [
        {"item": "Pasaporte" if d == "MAD" else "DNI", "motivo": "Obligatorio para viajar"},
        {"item": f"{min(noches, 7)} mudas de ropa", "motivo": f"{noches} noches de viaje"},
        {"item": "Cargador y batería externa", "motivo": "Fotos y mapas todo el día"},
        {"item": "Botiquín básico", "motivo": "Por las dudas"},
    ]
    if c:
        if c["min"] < 5:
            items += [{"item": "Campera de abrigo", "motivo": f"Mínimas de {c['min']}°C"},
                      {"item": "Gorro, guantes y ropa térmica", "motivo": "Frío intenso"}]
        elif c["min"] < 12:
            items.append({"item": "Abrigo liviano", "motivo": f"Las noches bajan a {c['min']}°C"})
        if c["max"] > 25:
            items += [{"item": "Ropa liviana", "motivo": f"Máximas de {c['max']}°C"},
                      {"item": "Protector solar y gorra", "motivo": "Sol fuerte"}]
        if c["dias_lluvia"] > c["dias"] / 3:
            items.append({"item": "Campera impermeable o paraguas",
                          "motivo": f"{c['dias_lluvia']} de {c['dias']} días con lluvia"})
    else:
        items.append({"item": "Revisá el pronóstico antes de salir", "motivo": "Sin datos de clima por ahora"})
    if d in ("BRC", "USH", "FTE") or "aventura" in preferencias or "naturaleza" in preferencias:
        items.append({"item": "Calzado de trekking", "motivo": "Senderos y montaña"})
    if d == "IGR":
        items += [{"item": "Repelente de insectos", "motivo": "Selva misionera"},
                  {"item": "Ropa que se pueda mojar", "motivo": "Las cataratas salpican"}]
    if d == "MDZ":
        items.append({"item": "Protector solar", "motivo": "Mucho sol en altura"})
    if d == "MAD":
        items.append({"item": "Adaptador de enchufe (tipo C/F)", "motivo": "España usa otro enchufe"})
    vistos, unicos = set(), []
    for i in items:
        if i["item"] not in vistos:
            vistos.add(i["item"])
            unicos.append(i)
    return unicos


def aeropuerto(destino: str) -> Optional[dict]:
    """Aeropuerto del destino para el mapa: nombre, direccion y coordenadas (None si no esta cargado)."""
    a = AEROPUERTOS.get(destino.upper())
    if not a:
        return None
    nombre, direccion, lat, lon = a
    return {"nombre": nombre, "direccion": direccion, "lat": lat, "lon": lon}


def itinerario(destino: str, desde: date, hasta: date, recos: List[dict]) -> List[dict]:
    dias = max(1, (hasta - desde).days) + 1
    ciudad = DESTINOS.get(destino.upper(), (destino,))[0]
    aero = aeropuerto(destino)
    en_aeropuerto = {"tipo": "aeropuerto", "lat": aero["lat"], "lon": aero["lon"]} if aero else {}
    plan = []
    actividades = list(recos)
    for n in range(dias):
        f = desde + timedelta(days=n)
        ubicacion = {}
        if n == 0:
            titulo, detalle = f"Llegada a {ciudad}", "Check-in, recorrida tranquila y cena"
            ubicacion = en_aeropuerto
        elif n == dias - 1:
            titulo, detalle = "Regreso", "Check-out y traslado al aeropuerto"
            ubicacion = en_aeropuerto
        elif actividades:
            a = actividades.pop(0)
            titulo, detalle = a["nombre"], f"{CATEGORIAS.get(a['categoria'], a['categoria']).capitalize()} · {a['motivo']}"
            exc = recomendaciones.ubicacion(titulo)
            ubicacion = {"tipo": "excursion", **exc} if exc else {}
        else:
            titulo, detalle = "Día libre", "Descanso o repetir lo que más te gustó"
        # Los dias con lugar (aeropuerto o excursion) llevan lat/lon para el mapa
        plan.append({"dia": n + 1, "fecha": f.isoformat(), "titulo": titulo, "detalle": detalle, **ubicacion})
    return plan


def mapa(destino: str, recos: List[dict], plan: List[dict]) -> Optional[dict]:
    """Centro del destino + excursiones recomendadas que no entraron en el itinerario (pines secundarios)."""
    d = destino.upper()
    if d not in DESTINOS:
        return None
    ciudad, lat, lon = DESTINOS[d]
    en_plan = {p["titulo"] for p in plan}
    extras = [{"nombre": r["nombre"], "categoria": r["categoria"], "lat": r["lat"], "lon": r["lon"]}
              for r in recos if "lat" in r and r["nombre"] not in en_plan]
    return {"ciudad": ciudad, "centro": {"lat": lat, "lon": lon}, "aeropuerto": aeropuerto(d),
            "otrasExcursiones": extras}


def generar(destino: str, desde: date, hasta: date, pax: int = 2, preferencias: Optional[List[str]] = None) -> dict:
    prefs = [p.strip().lower() for p in (preferencias or []) if p.strip()]
    c = consultar_clima(destino, desde, hasta)
    noches = max(1, (hasta - desde).days)
    recos = recomendaciones.recomendar(destino, prefs, c, top=10)
    plan = itinerario(destino, desde, hasta, recos)
    return {
        "destino": destino.upper(),
        "clima": c,
        "equipaje": equipaje(destino, c, noches, prefs),
        "itinerario": plan,
        "mapa": mapa(destino, recos, plan),
        "libros": libros.sugerir(destino),
        "pasajeros": pax,
    }
