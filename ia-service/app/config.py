"""Configuracion externalizada por variables de entorno."""
import os

RABBIT_HOST = os.getenv("RABBIT_HOST", "localhost")
RABBIT_HABILITADO = os.getenv("RABBIT_HABILITADO", "true").lower() == "true"
DB_DSN = os.getenv("IA_DB_DSN", "postgresql://travelhub:travelhub@localhost:5432/ia_db")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434")
OLLAMA_MODELO = os.getenv("OLLAMA_MODELO", "llama3.2:1b")
LLM_TIMEOUT_S = float(os.getenv("LLM_TIMEOUT_S", "5"))  # RNF: resumen < 5 s, si no, plantilla
MODELO_VERSION = "iforest-zscore-v1"

# Servicios que usa el asistente para armar paquetes reales
CATALOGO_URL = os.getenv("CATALOGO_URL", "http://localhost:8081")
PRECIOS_URL = os.getenv("PRECIOS_URL", "http://localhost:8082")
SERVICIOS_TIMEOUT_S = float(os.getenv("SERVICIOS_TIMEOUT_S", "40"))

# Open Library (libros para el viaje). Piden identificarse con User-Agent y cachear.
OPEN_LIBRARY_URL = os.getenv("OPEN_LIBRARY_URL", "https://openlibrary.org")
USER_AGENT = os.getenv("IA_USER_AGENT", "TravelHub-TP-UADE/1.0 (trabajo practico academico)")

EXCHANGE = "travelhub.events"
DLX = "travelhub.dlx"
RK_PRECIO_CAMBIADO = "precio.cambiado"
RK_ANOMALIA = "anomalia.detectada"
COLA_PRECIOS = "ia.precio-cambiado"

# Coordenadas para el clima (Open-Meteo) y nombres de ciudad
DESTINOS = {
    "BRC": ("Bariloche", -41.13, -71.31),
    "MDZ": ("Mendoza", -32.89, -68.83),
    "IGR": ("Puerto Iguazu", -25.60, -54.57),
    "USH": ("Ushuaia", -54.80, -68.30),
    "MAD": ("Madrid", 40.42, -3.70),
    "BUE": ("Buenos Aires", -34.60, -58.38),
    "SLA": ("Salta", -24.79, -65.41),
    "FTE": ("El Calafate", -50.34, -72.26),
}

# Destinos que hoy vende el catalogo (los que el asistente puede ofrecer)
DESTINOS_VENTA = ["BRC", "MDZ", "IGR", "USH", "MAD"]
