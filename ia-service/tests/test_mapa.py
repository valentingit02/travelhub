"""Mapa del kit de viaje: cada excursion tiene ubicacion, cerca de su destino, y el kit la devuelve."""
from datetime import date
from math import asin, cos, radians, sin, sqrt

import app.kit as kit
import app.libros as libros
from app.config import AEROPUERTOS, DESTINOS
from app.recomendaciones import EXCURSIONES, UBICACIONES


def _km(a, b):
    """Distancia en km entre dos (lat, lon) (formula de haversine)."""
    lat1, lon1, lat2, lon2 = map(radians, (*a, *b))
    h = sin((lat2 - lat1) / 2) ** 2 + cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2) ** 2
    return 2 * 6371 * asin(sqrt(h))


def test_todas_las_excursiones_tienen_ubicacion_cerca_de_su_destino():
    for destino, excursiones in EXCURSIONES.items():
        _, lat, lon = DESTINOS[destino]
        for nombre, _, _ in excursiones:
            assert nombre in UBICACIONES, f"{nombre} no tiene ubicacion"
            # Toledo queda a ~70 km de Madrid y el Aconcagua a ~100 km de Mendoza: 150 km es el tope razonable
            assert _km((lat, lon), UBICACIONES[nombre]) < 150, f"{nombre} queda lejos de {destino}"


def test_el_kit_trae_ubicacion_en_los_dias_con_excursion(monkeypatch):
    monkeypatch.setattr(kit, "consultar_clima", lambda *a: None)
    monkeypatch.setattr(libros, "sugerir", lambda d, limite=5: [])

    k = kit.generar("BRC", date(2027, 7, 10), date(2027, 7, 13), preferencias=["aventura"])
    plan = k["itinerario"]

    # llegada y regreso: en el aeropuerto del destino
    for d in (plan[0], plan[-1]):
        assert d["tipo"] == "aeropuerto" and (d["lat"], d["lon"]) == AEROPUERTOS["BRC"][2:]
    con_pin = [d for d in plan if d.get("tipo") == "excursion"]
    assert con_pin and con_pin[0]["titulo"] == "Cerro Catedral"    # la preferencia "aventura" va primero
    assert (con_pin[0]["lat"], con_pin[0]["lon"]) == UBICACIONES["Cerro Catedral"]


def test_el_mapa_tiene_centro_y_no_repite_excursiones_del_itinerario(monkeypatch):
    monkeypatch.setattr(kit, "consultar_clima", lambda *a: None)
    monkeypatch.setattr(libros, "sugerir", lambda d, limite=5: [])

    k = kit.generar("BRC", date(2027, 7, 10), date(2027, 7, 13))
    m = k["mapa"]

    assert m["ciudad"] == "Bariloche"
    assert m["centro"] == {"lat": DESTINOS["BRC"][1], "lon": DESTINOS["BRC"][2]}
    en_plan = {d["titulo"] for d in k["itinerario"]}
    otras = {e["nombre"] for e in m["otrasExcursiones"]}
    assert otras and not (otras & en_plan)
    assert len(otras) + len([d for d in k["itinerario"] if d.get("tipo") == "excursion"]) == len(EXCURSIONES["BRC"])
    assert m["aeropuerto"]["nombre"] == AEROPUERTOS["BRC"][0] and m["aeropuerto"]["direccion"]


def test_destino_sin_coordenadas_no_rompe_el_kit(monkeypatch):
    monkeypatch.setattr(kit, "consultar_clima", lambda *a: None)
    monkeypatch.setattr(libros, "sugerir", lambda d, limite=5: [])
    k = kit.generar("XXX", date(2027, 7, 10), date(2027, 7, 12))
    assert k["mapa"] is None
    assert all("lat" not in d for d in k["itinerario"])


def test_cada_destino_tiene_su_aeropuerto_cerca():
    assert set(AEROPUERTOS) == set(DESTINOS)
    for destino, (ciudad, lat, lon) in DESTINOS.items():
        nombre, direccion, a_lat, a_lon = AEROPUERTOS[destino]
        assert nombre and direccion
        # Los aeropuertos de estos destinos estan a menos de 30 km del centro (Barajas ~13 km, El Calafate ~20 km)
        assert _km((lat, lon), (a_lat, a_lon)) < 30, f"el aeropuerto de {ciudad} queda lejos"
