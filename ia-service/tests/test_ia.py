from datetime import date, timedelta

import numpy as np
from fastapi.testclient import TestClient

import app.resumen as resumen
from app.anomalias import MIN_MUESTRAS, DetectorAnomalias, detector
from app.main import app
from app.mensajeria import procesar
from app.recomendaciones import recomendar
from evaluar_modelo import escenario_historial, escenario_referencia, escenario_variacion

cliente = TestClient(app)


def test_cumple_rnf_en_los_tres_escenarios():
    for m in (escenario_referencia(), escenario_historial(), escenario_variacion()):
        assert m["recall"] >= 0.85
        assert m["precision"] >= 0.85
        assert m["latencia_p95_ms"] < 200


def test_auto_a_un_dolar_es_anomalia():
    assert detector.evaluar("AUTO", 1.0).es_anomalia
    assert not detector.evaluar("AUTO", 80.0).es_anomalia


def test_vuelo_caro_a_madrid_sin_historial_no_se_bloquea():
    assert not DetectorAnomalias().evaluar("VUELO", 900.0, "MAD").es_anomalia


def test_variacion_brusca_respecto_del_anterior():
    r = DetectorAnomalias().evaluar("AUTO", 110.0, "BRC", precio_anterior=15.0)
    assert r.es_anomalia and r.fuente == "variacion"


def test_reentrena_con_historial_por_destino():
    det = DetectorAnomalias()
    rng = np.random.default_rng(1)
    obs = [("HOTEL", "MAD", float(p)) for p in np.exp(rng.normal(np.log(400), 0.15, MIN_MUESTRAS + 10))]
    assert det.entrenar_con_historial(obs) == 1
    assert det.evaluar("HOTEL", 400.0, "MAD").fuente == "historial MAD"
    assert det.evaluar("HOTEL", 1500.0, "MAD").es_anomalia        # para la referencia global seria normal
    assert not det.evaluar("HOTEL", 400.0, "MAD").es_anomalia


def test_endpoint_evaluar():
    r = cliente.post("/api/ia/anomalias/evaluar", json={"tipoProducto": "HOTEL", "precio": 5000})
    assert r.status_code == 200
    assert r.json()["esAnomalia"] is True


def test_endpoint_modelo():
    r = cliente.get("/api/ia/anomalias/modelo")
    assert r.status_code == 200
    assert "referencia" in r.json()


def test_consumidor_publica_solo_si_hay_anomalia():
    base = {"productoRef": "AUTO-7", "tipoProducto": "AUTO", "destino": "BRC", "moneda": "USD"}
    assert procesar(base | {"precioNuevo": 85, "precioAnterior": 80}) is None
    salida = procesar(base | {"precioNuevo": 1, "precioAnterior": 120})
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
