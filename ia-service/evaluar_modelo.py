"""
Validacion del componente de IA (para el informe). Uso:  python evaluar_modelo.py

Escenario A - sin historial: solo la distribucion de referencia (umbral conservador).
Escenario B - con historial: un grupo tipo+destino entrenado con precios reales simulados.
Escenario C - variacion: cambios bruscos respecto del precio anterior.
Se inyectan anomalias tipicas de error de carga (precio /100 o x10) y se mide
matriz de confusion, precision, recall y latencia p95.
"""
import time

import numpy as np

from app.anomalias import REFERENCIA, DetectorAnomalias


def _medir(casos, evaluar):
    vp = fp = vn = fn = 0
    tiempos = []
    for args, real in casos:
        t0 = time.perf_counter()
        pred = evaluar(*args).es_anomalia
        tiempos.append((time.perf_counter() - t0) * 1000)
        if pred and real: vp += 1
        elif pred and not real: fp += 1
        elif not pred and real: fn += 1
        else: vn += 1
    return {"vp": vp, "fp": fp, "vn": vn, "fn": fn,
            "precision": vp / (vp + fp) if vp + fp else 0.0,
            "recall": vp / (vp + fn) if vp + fn else 0.0,
            "latencia_p95_ms": float(np.percentile(tiempos, 95))}


def _anomalo(rng, base):
    return float(base * rng.choice([rng.uniform(0.005, 0.12), rng.uniform(8, 40)]))


def escenario_referencia(semilla=7, normales=400, anomalias=60):
    rng = np.random.default_rng(semilla)
    casos = []
    for tipo, (mediana, sigma) in REFERENCIA.items():
        casos += [((tipo, float(p)), False) for p in np.exp(rng.normal(np.log(mediana), sigma, normales))]
        casos += [((tipo, _anomalo(rng, mediana)), True) for _ in range(anomalias)]
    return _medir(casos, DetectorAnomalias(semilla=42).evaluar)


def escenario_historial(semilla=11):
    rng = np.random.default_rng(semilla)
    det = DetectorAnomalias(semilla=42)
    mediana, sigma = 420.0, 0.20            # hoteles de un destino caro, con poca dispersion
    det.entrenar_con_historial([("HOTEL", "MAD", float(p)) for p in np.exp(rng.normal(np.log(mediana), sigma, 200))])
    casos = [(("HOTEL", float(p), "MAD"), False) for p in np.exp(rng.normal(np.log(mediana), sigma, 300))]
    casos += [(("HOTEL", float(mediana * f), "MAD"), True) for f in rng.uniform(2.3, 6, 30)]   # x2.3 a x6
    casos += [(("HOTEL", float(mediana * f), "MAD"), True) for f in rng.uniform(0.05, 0.4, 30)]
    return _medir(casos, det.evaluar)


def escenario_variacion(semilla=13):
    rng = np.random.default_rng(semilla)
    det = DetectorAnomalias(semilla=42)
    casos = []
    for _ in range(200):
        anterior = float(rng.uniform(40, 150))
        casos.append((("AUTO", anterior * float(rng.uniform(0.7, 1.4)), "BRC", anterior), False))
        casos.append((("AUTO", anterior * float(rng.choice([rng.uniform(0.01, 0.15), rng.uniform(6, 20)])),
                       "BRC", anterior), True))
    return _medir(casos, det.evaluar)


def evaluar():
    return escenario_referencia()


def _imprimir(titulo, m):
    print(f"\n=== {titulo} ===")
    print("                 Pred anomalia   Pred normal")
    print(f"Real anomalia    {m['vp']:>13}   {m['fn']:>11}")
    print(f"Real normal      {m['fp']:>13}   {m['vn']:>11}")
    print(f"Precision: {m['precision']:.3f}   Recall: {m['recall']:.3f} (RNF >= 0.85)   "
          f"Latencia p95: {m['latencia_p95_ms']:.2f} ms (RNF < 200 ms)")


if __name__ == "__main__":
    _imprimir("A. Sin historial (referencia, umbral z>=4)", escenario_referencia())
    _imprimir("B. Con historial propio del destino (z>=3 + Isolation Forest)", escenario_historial())
    _imprimir("C. Variacion respecto del precio anterior", escenario_variacion())
