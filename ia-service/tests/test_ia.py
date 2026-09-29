from datetime import date, timedelta

from fastapi.testclient import TestClient

import app.resumen as resumen
from app.anomalias import detector
from app.main import app
from app.mensajeria import procesar
from app.recomendaciones import recomendar
from evaluar_modelo import evaluar

cliente = TestClient(app)


def test_cumple_rnf_de_exactitud_y_latencia():
    m = evaluar()
    assert m["recall"] >= 0.85
    assert m["precision"] >= 0.80
    assert m["latencia_p95_ms"] < 200


def test_auto_a_un_dolar_es_anomalia():
    assert detector.evaluar("AUTO", 1.0).es_anomalia
    assert not detector.evaluar("AUTO", 80.0).es_anomalia


def test_endpoint_evaluar():
    r = cliente.post("/api/ia/anomalias/evaluar", json={"tipoProducto": "HOTEL", "precio": 5000})
    assert r.status_code == 200
    assert r.json()["esAnomalia"] is True


def test_consumidor_publica_solo_si_hay_anomalia():
    base = {"productoRef": "AUTO-7", "tipoProducto": "AUTO", "destino": "BRC", "moneda": "USD"}
    assert procesar(base | {"precioNuevo": 85}) is None
    salida = procesar(base | {"precioNuevo": 1})
    assert salida["productoRef"] == "AUTO-7"


def test_resumen_cae_a_plantilla_si_no_hay_llm(monkeypatch):
    monkeypatch.setattr(resumen, "OLLAMA_URL", "http://127.0.0.1:9")  # puerto cerrado
    monkeypatch.setattr("app.main.clima", lambda *a: None)
    desde = date.today() + timedelta(days=30)
    r = cliente.post("/api/ia/resumen", json={
        "nombre": "Ana", "destino": "BRC", "desde": desde.isoformat(),
        "hasta": (desde + timedelta(days=4)).isoformat(), "pasajeros": 2,
        "items": [{"tipo": "HOTEL", "descripcion": "Llao Llao", "precio": 500}],
        "total": 500, "moneda": "USD"})
    assert r.status_code == 200
    assert r.json()["generadoPor"] == "plantilla"
    assert "Bariloche" in r.json()["resumen"]


def test_prompt_no_incluye_datos_sensibles():
    p = resumen.prompt({"nombre": "Ana", "destino": "BRC", "desde": "2027-07-10", "hasta": "2027-07-14",
                        "pasajeros": 2, "items": [], "total": 1, "moneda": "USD"}, None)
    assert "@" not in p and "documento" not in p.lower()


def test_con_lluvia_prioriza_bajo_techo():
    lluvia = {"max": 12, "min": 4, "dias_lluvia": 4, "dias": 5}
    top = recomendar("BRC", [], lluvia, top=1)[0]
    assert top["alAireLibre"] is False
