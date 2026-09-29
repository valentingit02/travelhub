from datetime import date

import pytest

import app.asistente as asistente
import app.kit as kit
import app.libros as libros

HOY = date(2026, 9, 29)


def test_nieve_en_julio_con_mi_novia():
    r = asistente.interpretar("Quiero ir a la nieve en julio con mi novia", HOY)
    assert r["destino"] == "BRC"
    assert r["desde"] == "2027-07-10"
    assert r["hasta"] == "2027-07-15"
    assert r["pax"] == 2


def test_fechas_explicitas_y_amigos():
    r = asistente.interpretar("Mendoza del 12 al 16 de noviembre con 3 amigos, nos gusta el vino", HOY)
    assert r["destino"] == "MDZ"
    assert (r["desde"], r["hasta"]) == ("2026-11-12", "2026-11-16")
    assert r["pax"] == 4
    assert "gastronomia" in r["intereses"]


def test_fin_de_semana_y_presupuesto():
    r = asistente.interpretar("un finde en las cataratas, presupuesto 900 dolares", HOY)
    assert r["destino"] == "IGR"
    assert r["noches"] == 2
    assert r["presupuesto"] == 900
    assert date.fromisoformat(r["desde"]).weekday() == 4   # viernes


def test_playa_no_esta_disponible():
    r = asistente.interpretar("quiero playa y surf", HOY)
    assert r.get("sinOferta") and "destino" not in r


def test_contexto_se_combina():
    base = asistente.interpretar("Ushuaia en agosto", HOY)
    r = asistente.combinar(base, asistente.interpretar("somos 4 y queremos auto", HOY))
    assert r["destino"] == "USH" and r["pax"] == 4 and r["auto"] is True


def test_pide_destino_si_falta(monkeypatch):
    monkeypatch.setattr(asistente, "_interpretar_llm", lambda m: {})
    r = asistente.responder("quiero viajar", None, HOY)
    assert r["faltantes"] == ["destino"] and r["sugerencia"] is None


def test_arma_paquete_con_servicios(monkeypatch):
    prod = lambda i, t, n, p, det=None: {"id": i, "tipo": t, "nombre": n, "precioBase": p, "moneda": "USD",
                                        "ocupacion": 0.5, "detalle": det or {}}
    catalogo = {"vuelos": [prod("v2", "VUELO", "Vuelo caro", 200), prod("v1", "VUELO", "Vuelo barato", 150)],
                "hoteles": [prod("MOCK-HOTEL-FALLA", "HOTEL", "Falla", 10), prod("h1", "HOTEL", "Hotel Lago", 120)],
                "excursiones": [prod("e1", "EXCURSION", "Circuito Chico", 40, {"categoria": "naturaleza"})],
                "autos": []}
    monkeypatch.setattr(asistente, "buscar_catalogo", lambda i: catalogo)
    monkeypatch.setattr(asistente, "consultar_clima", lambda *a: None)
    monkeypatch.setattr(asistente, "cotizar", lambda productos, i: {
        "items": [{"factores": [{"estrategia": "TEMPORADA", "factor": 1.3}]}], "subtotal": 1000,
        "total": 930, "moneda": "USD", "factorPaquete": 0.93, "motivoDescuento": "Descuento por paquete"})
    r = asistente.responder("Bariloche en julio 5 noches con mi pareja", None, HOY)
    ids = [p["id"] for p in r["sugerencia"]["productos"]]
    assert ids == ["v1", "h1", "e1"]            # vuelo mas barato, sin el hotel de demo de Saga
    assert "temporada alta" in r["respuesta"]
    assert r["sugerencia"]["busqueda"]["pax"] == 2


def test_si_el_catalogo_falla_no_se_cae(monkeypatch):
    def falla(i):
        raise RuntimeError("caido")
    monkeypatch.setattr(asistente, "buscar_catalogo", falla)
    r = asistente.responder("Madrid en octubre", None, HOY)
    assert "No pude consultar" in r["respuesta"]


def test_kit_de_viaje_con_frio_y_lluvia(monkeypatch):
    monkeypatch.setattr(kit, "consultar_clima", lambda *a: {"min": 1, "max": 8, "dias_lluvia": 3, "dias": 5,
                                                           "fuente": "test"})
    monkeypatch.setattr(libros, "sugerir", lambda d, limite=5: [{"titulo": "En la Patagonia"}])
    k = kit.generar("BRC", date(2027, 7, 10), date(2027, 7, 15))
    items = [x["item"] for x in k["equipaje"]]
    assert "Campera de abrigo" in items and "Campera impermeable o paraguas" in items
    assert len(k["itinerario"]) == 6 and k["itinerario"][-1]["titulo"] == "Regreso"


def test_libros_usan_respaldo_si_open_library_no_responde(monkeypatch):
    monkeypatch.setattr(libros, "OPEN_LIBRARY_URL", "http://127.0.0.1:9")
    libros._CACHE.clear()
    l = libros.sugerir("IGR", timeout=1)
    assert l and l[0]["autor"] == "Horacio Quiroga"
