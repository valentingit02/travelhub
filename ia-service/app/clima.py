"""Clima del destino con Open-Meteo (gratis, sin API key).

Si el viaje es dentro de 15 dias se usa el pronostico; si es mas adelante,
se usa el clima real de las mismas fechas del año anterior como estimacion.
"""
from datetime import date, timedelta
from typing import Optional

import httpx

from .config import DESTINOS


def clima(destino: str, desde: date, hasta: date, timeout: float = 4.0) -> Optional[dict]:
    if destino.upper() not in DESTINOS:
        return None
    _, lat, lon = DESTINOS[destino.upper()]
    hasta = min(hasta, desde + timedelta(days=13))
    params = {
        "latitude": lat, "longitude": lon, "timezone": "auto",
        "daily": "temperature_2m_max,temperature_2m_min,precipitation_sum",
    }
    try:
        if (desde - date.today()).days <= 15:
            url = "https://api.open-meteo.com/v1/forecast"
            params |= {"start_date": desde.isoformat(), "end_date": hasta.isoformat()}
            fuente = "pronostico"
        else:
            url = "https://archive-api.open-meteo.com/v1/archive"
            d, h = _anio_anterior(desde), _anio_anterior(hasta)
            params |= {"start_date": d.isoformat(), "end_date": h.isoformat()}
            fuente = "mismas fechas del año anterior"
        r = httpx.get(url, params=params, timeout=timeout)
        r.raise_for_status()
        daily = r.json()["daily"]
        maximas = [t for t in daily["temperature_2m_max"] if t is not None]
        minimas = [t for t in daily["temperature_2m_min"] if t is not None]
        lluvia = [p for p in daily["precipitation_sum"] if p is not None]
        if not maximas:
            return None
        return {
            "fuente": fuente,
            "max": round(sum(maximas) / len(maximas), 1),
            "min": round(sum(minimas) / len(minimas), 1),
            "dias_lluvia": sum(1 for p in lluvia if p >= 1.0),
            "dias": len(maximas),
        }
    except Exception:
        return None


def _anio_anterior(d: date) -> date:
    try:
        return d.replace(year=d.year - 1)
    except ValueError:  # 29 de febrero
        return d.replace(year=d.year - 1, day=28)
