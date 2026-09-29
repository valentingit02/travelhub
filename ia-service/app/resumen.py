"""Resumen del viaje con un LLM local (Ollama). Si tarda o falla, se usa una plantilla."""
import time
from typing import Optional

import httpx

from .config import LLM_TIMEOUT_S, OLLAMA_MODELO, OLLAMA_URL, DESTINOS


def _ciudad(iata: str) -> str:
    return DESTINOS.get(iata.upper(), (iata,))[0]


def _texto_clima(c: Optional[dict]) -> str:
    if not c:
        return ""
    return (f"Clima esperado ({c['fuente']}): máximas de {c['max']}°C, mínimas de {c['min']}°C "
            f"y {c['dias_lluvia']} de {c['dias']} días con lluvia.")


def plantilla(datos: dict, clima: Optional[dict]) -> str:
    items = "\n".join(f"- {i['tipo'].capitalize()}: {i['descripcion']}" for i in datos["items"])
    abrigo = ""
    if clima and clima["min"] < 5:
        abrigo = " Llevá buen abrigo: las mínimas son bajas."
    elif clima and clima["dias_lluvia"] > clima["dias"] / 3:
        abrigo = " No te olvides el paraguas."
    return (f"¡Hola {datos['nombre']}! Tu viaje a {_ciudad(datos['destino'])} del {datos['desde']} "
            f"al {datos['hasta']} para {datos['pasajeros']} persona(s) está confirmado.\n"
            f"{items}\nTotal: {datos['total']} {datos['moneda']}.\n{_texto_clima(clima)}{abrigo}").strip()


def prompt(datos: dict, clima: Optional[dict]) -> str:
    # Privacidad: el prompt solo lleva nombre de pila, destino, fechas y productos.
    items = "; ".join(f"{i['tipo']}: {i['descripcion']}" for i in datos["items"])
    return (
        "Sos el asistente de una agencia de viajes argentina. Escribí en español rioplatense, "
        "en tono cálido y en no más de 90 palabras, un resumen del viaje confirmado con 2 consejos "
        "prácticos según el clima. No inventes datos que no estén acá.\n"
        f"Viajero: {datos['nombre']}. Destino: {_ciudad(datos['destino'])}. "
        f"Fechas: {datos['desde']} a {datos['hasta']}. Pasajeros: {datos['pasajeros']}.\n"
        f"Incluye: {items}. Total: {datos['total']} {datos['moneda']}.\n{_texto_clima(clima)}"
    )


def generar(datos: dict, clima: Optional[dict]) -> dict:
    inicio = time.perf_counter()
    try:
        r = httpx.post(f"{OLLAMA_URL}/api/generate",
                       json={"model": OLLAMA_MODELO, "prompt": prompt(datos, clima), "stream": False,
                             "options": {"temperature": 0.4, "num_predict": 180}},
                       timeout=LLM_TIMEOUT_S)
        r.raise_for_status()
        texto = r.json().get("response", "").strip()
        if not texto:
            raise ValueError("respuesta vacia")
        return {"resumen": texto, "generadoPor": f"llm:{OLLAMA_MODELO}",
                "latenciaMs": int((time.perf_counter() - inicio) * 1000)}
    except Exception:
        # Degradacion elegante: si el LLM no responde a tiempo, plantilla
        return {"resumen": plantilla(datos, clima), "generadoPor": "plantilla",
                "latenciaMs": int((time.perf_counter() - inicio) * 1000)}
