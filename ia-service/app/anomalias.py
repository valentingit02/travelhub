"""
Deteccion de anomalias de precio.

Modelo: por cada tipo de producto se entrena un Isolation Forest sobre el log del precio
de un historico de referencia, y se combina con una regla z-score (respaldo explicable).
Un precio es anomalo si el z-score del log supera el umbral, o si el Isolation Forest lo
aisla y ademas esta razonablemente lejos de la media.
"""
from dataclasses import dataclass

import numpy as np
from sklearn.ensemble import IsolationForest

from .config import MODELO_VERSION

# Distribucion de referencia por tipo (USD): mediana y dispersion (en log)
REFERENCIA = {
    "VUELO": (180.0, 0.45),
    "HOTEL": (110.0, 0.40),
    "EXCURSION": (55.0, 0.40),
    "AUTO": (75.0, 0.35),
}
UMBRAL_Z = 3.0
UMBRAL_Z_IFOREST = 2.2


@dataclass
class Resultado:
    es_anomalia: bool
    score: float
    z: float
    motivo: str
    modelo_version: str = MODELO_VERSION


class DetectorAnomalias:
    def __init__(self, semilla: int = 42, muestras: int = 600):
        rng = np.random.default_rng(semilla)
        self.stats = {}
        self.modelos = {}
        for tipo, (mediana, sigma) in REFERENCIA.items():
            log_precios = rng.normal(np.log(mediana), sigma, muestras)
            self.stats[tipo] = (float(log_precios.mean()), float(log_precios.std()))
            modelo = IsolationForest(n_estimators=150, contamination=0.02, random_state=semilla)
            modelo.fit(log_precios.reshape(-1, 1))
            self.modelos[tipo] = modelo

    def evaluar(self, tipo: str, precio: float) -> Resultado:
        tipo = tipo.upper()
        if tipo not in self.modelos:
            return Resultado(False, 0.0, 0.0, f"Tipo {tipo} sin modelo: no se evalua")
        if precio <= 0:
            return Resultado(True, -1.0, float("inf"), "Precio no positivo")

        media, desvio = self.stats[tipo]
        x = np.log(precio)
        z = (x - media) / desvio
        score = float(self.modelos[tipo].decision_function([[x]])[0])  # < 0 = aislado
        aislado = self.modelos[tipo].predict([[x]])[0] == -1

        if abs(z) >= UMBRAL_Z:
            motivo = f"Precio {'muy bajo' if z < 0 else 'muy alto'} para {tipo} (z={z:.1f})"
            return Resultado(True, score, float(z), motivo)
        if aislado and abs(z) >= UMBRAL_Z_IFOREST:
            return Resultado(True, score, float(z), f"Isolation Forest aislo el precio (z={z:.1f})")
        return Resultado(False, score, float(z), "Precio dentro del rango esperado")


detector = DetectorAnomalias()
