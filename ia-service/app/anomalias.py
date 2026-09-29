"""
Deteccion de anomalias de precio (v2).

Tres capas, de la mas especifica a la mas general:
 1. Variacion: si el producto ya tenia precio y cambio mas de x5 o a menos del 20 %, es anomalo.
 2. Historial propio: si hay >= MIN_MUESTRAS precios reales del mismo tipo y destino, se entrena
    un Isolation Forest + z-score con ellos (umbral estricto).
 3. Referencia: sin historial suficiente se usa una distribucion de referencia por tipo, con un
    umbral mas conservador para no bloquear destinos caros (ej. vuelos a Madrid).
"""
import threading
from collections import defaultdict
from dataclasses import dataclass
from typing import Iterable, Optional, Tuple

import numpy as np
from sklearn.ensemble import IsolationForest

# Distribucion de referencia por tipo (USD): mediana y dispersion (en log)
REFERENCIA = {
    "VUELO": (180.0, 0.45),
    "HOTEL": (110.0, 0.40),
    "EXCURSION": (55.0, 0.40),
    "AUTO": (75.0, 0.35),
}
UMBRAL_Z_HISTORIAL = 3.0
UMBRAL_Z_IFOREST = 2.6
UMBRAL_Z_REFERENCIA = 4.0
MIN_MUESTRAS = 20
DESVIO_MINIMO = 0.15
VARIACION_MIN, VARIACION_MAX = 0.2, 5.0


@dataclass
class Resultado:
    es_anomalia: bool
    score: float
    z: float
    motivo: str
    modelo_version: str
    fuente: str = "referencia"


@dataclass
class _Modelo:
    media: float
    desvio: float
    bosque: IsolationForest
    muestras: int


def _entrenar(log_precios: np.ndarray, semilla: int) -> _Modelo:
    bosque = IsolationForest(n_estimators=150, contamination=0.02, random_state=semilla)
    bosque.fit(log_precios.reshape(-1, 1))
    return _Modelo(float(log_precios.mean()), max(float(log_precios.std()), DESVIO_MINIMO), bosque, len(log_precios))


class DetectorAnomalias:
    def __init__(self, semilla: int = 42, muestras: int = 600):
        self.semilla = semilla
        rng = np.random.default_rng(semilla)
        self.referencia = {
            tipo: _entrenar(rng.normal(np.log(mediana), sigma, muestras), semilla)
            for tipo, (mediana, sigma) in REFERENCIA.items()
        }
        self.grupos: dict = {}
        self.entrenamientos = 0
        self._lock = threading.Lock()

    @property
    def version(self) -> str:
        return f"v2-ref+{len(self.grupos)}grupos-e{self.entrenamientos}"

    def entrenar_con_historial(self, observaciones: Iterable[Tuple[str, str, float]]) -> int:
        """observaciones: (tipo, destino, precio_usd) de precios considerados normales."""
        por_grupo = defaultdict(list)
        for tipo, destino, precio in observaciones:
            if precio and precio > 0:
                por_grupo[(tipo.upper(), (destino or "").upper())].append(np.log(float(precio)))
        nuevos = {k: _entrenar(np.array(v), self.semilla) for k, v in por_grupo.items() if len(v) >= MIN_MUESTRAS}
        with self._lock:
            self.grupos = nuevos
            self.entrenamientos += 1
        return len(nuevos)

    def resumen(self) -> dict:
        return {
            "version": self.version,
            "minMuestrasPorGrupo": MIN_MUESTRAS,
            "referencia": {t: {"medianaUsd": round(float(np.exp(m.media)), 2), "desvioLog": round(m.desvio, 3)}
                           for t, m in self.referencia.items()},
            "grupos": [{"tipo": t, "destino": d, "muestras": m.muestras,
                        "medianaUsd": round(float(np.exp(m.media)), 2), "desvioLog": round(m.desvio, 3)}
                       for (t, d), m in sorted(self.grupos.items())],
        }

    def evaluar(self, tipo: str, precio: float, destino: Optional[str] = None,
                precio_anterior: Optional[float] = None) -> Resultado:
        tipo = tipo.upper()
        if precio <= 0:
            return Resultado(True, -1.0, float("inf"), "Precio no positivo", self.version, "regla")

        if precio_anterior and precio_anterior > 0:
            ratio = precio / float(precio_anterior)
            if ratio < VARIACION_MIN or ratio > VARIACION_MAX:
                pct = round((ratio - 1) * 100)
                motivo = f"El precio {'bajo' if ratio < 1 else 'subio'} {abs(pct)} % respecto del anterior"
                return Resultado(True, -1.0, 0.0, motivo, self.version, "variacion")

        clave = (tipo, (destino or "").upper())
        with self._lock:
            modelo = self.grupos.get(clave)
        if modelo is not None:
            umbral, fuente = UMBRAL_Z_HISTORIAL, f"historial {clave[1]}"
        elif tipo in self.referencia:
            modelo, umbral, fuente = self.referencia[tipo], UMBRAL_Z_REFERENCIA, "referencia"
        else:
            return Resultado(False, 0.0, 0.0, f"Tipo {tipo} sin modelo: no se evalua", self.version, "ninguna")

        x = np.log(precio)
        z = (x - modelo.media) / modelo.desvio
        score = float(modelo.bosque.decision_function([[x]])[0])  # < 0 = aislado
        aislado = modelo.bosque.predict([[x]])[0] == -1

        if abs(z) >= umbral:
            motivo = f"Precio {'muy bajo' if z < 0 else 'muy alto'} para {tipo} segun {fuente} (z={z:.1f})"
            return Resultado(True, score, float(z), motivo, self.version, fuente)
        if fuente != "referencia" and aislado and abs(z) >= UMBRAL_Z_IFOREST:
            return Resultado(True, score, float(z), f"Isolation Forest aislo el precio ({fuente}, z={z:.1f})",
                             self.version, fuente)
        return Resultado(False, score, float(z), f"Precio dentro del rango esperado ({fuente})", self.version, fuente)


detector = DetectorAnomalias()
