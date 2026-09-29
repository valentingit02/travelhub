"""
Validacion del componente de IA (para el informe).
Genera un dataset de prueba con precios normales y anomalias inyectadas, y calcula
matriz de confusion, precision, recall y latencia. Uso:  python evaluar_modelo.py
"""
import time

import numpy as np

from app.anomalias import REFERENCIA, DetectorAnomalias


def dataset(semilla=7, normales=400, anomalias=60):
    rng = np.random.default_rng(semilla)
    datos = []
    for tipo, (mediana, sigma) in REFERENCIA.items():
        for p in np.exp(rng.normal(np.log(mediana), sigma, normales)):
            datos.append((tipo, float(p), False))
        for _ in range(anomalias):
            factor = rng.choice([rng.uniform(0.005, 0.12), rng.uniform(6, 40)])  # error de carga: /100 o x10
            datos.append((tipo, float(mediana * factor), True))
    return datos


def evaluar():
    detector = DetectorAnomalias(semilla=42)  # entrenado con otra semilla que el dataset de prueba
    vp = fp = vn = fn = 0
    tiempos = []
    for tipo, precio, real in dataset():
        t0 = time.perf_counter()
        pred = detector.evaluar(tipo, precio).es_anomalia
        tiempos.append((time.perf_counter() - t0) * 1000)
        if pred and real: vp += 1
        elif pred and not real: fp += 1
        elif not pred and real: fn += 1
        else: vn += 1
    precision = vp / (vp + fp) if vp + fp else 0
    recall = vp / (vp + fn) if vp + fn else 0
    return {"vp": vp, "fp": fp, "vn": vn, "fn": fn, "precision": precision, "recall": recall,
            "latencia_p95_ms": float(np.percentile(tiempos, 95))}


if __name__ == "__main__":
    m = evaluar()
    print("Matriz de confusion")
    print(f"                 Pred anomalia   Pred normal")
    print(f"Real anomalia    {m['vp']:>13}   {m['fn']:>11}")
    print(f"Real normal      {m['fp']:>13}   {m['vn']:>11}")
    print(f"\nPrecision: {m['precision']:.3f}")
    print(f"Recall:    {m['recall']:.3f}   (RNF: >= 0.85)")
    print(f"Latencia p95: {m['latencia_p95_ms']:.2f} ms   (RNF: < 200 ms)")
