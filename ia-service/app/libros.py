"""
Libros para el viaje: busqueda en Open Library (gratis, sin API key) por tema del destino.
Cache en memoria de 24 h y lista de respaldo curada si la API no responde.
"""
import time
from urllib.parse import quote_plus

import httpx

from .config import OPEN_LIBRARY_URL, USER_AGENT

TEMAS = {
    "BRC": "Patagonia",
    "USH": "Tierra del Fuego",
    "MDZ": "Mendoza Argentina",
    "IGR": "Misiones selva",
    "MAD": "Madrid",
    "SLA": "Salta Argentina",
    "FTE": "Patagonia glaciares",
}

RESPALDO = {
    "BRC": [("En la Patagonia", "Bruce Chatwin", 1977), ("Patagonia Express", "Luis Sepúlveda", 1995)],
    "USH": [("El último confín de la tierra", "E. Lucas Bridges", 1948),
            ("Viaje de un naturalista alrededor del mundo", "Charles Darwin", 1839)],
    "MDZ": [("Zama", "Antonio Di Benedetto", 1956)],
    "IGR": [("Cuentos de la selva", "Horacio Quiroga", 1918),
            ("Cuentos de amor de locura y de muerte", "Horacio Quiroga", 1917)],
    "MAD": [("Fortunata y Jacinta", "Benito Pérez Galdós", 1887)],
}

_CACHE: dict = {}
_TTL_S = 24 * 3600


def _respaldo(destino: str) -> list:
    return [{"titulo": t, "autor": a, "anio": y, "portada": None,
             "url": f"{OPEN_LIBRARY_URL}/search?q={quote_plus(t)}", "fuente": "seleccion propia"}
            for t, a, y in RESPALDO.get(destino.upper(), RESPALDO["BRC"])]


def sugerir(destino: str, limite: int = 5, timeout: float = 5.0) -> list:
    destino = destino.upper()
    en_cache = _CACHE.get(destino)
    if en_cache and time.time() - en_cache[0] < _TTL_S:
        return en_cache[1][:limite]
    tema = TEMAS.get(destino)
    if not tema:
        return _respaldo(destino)[:limite]
    try:
        r = httpx.get(f"{OPEN_LIBRARY_URL}/search.json",
                      params={"q": tema, "fields": "key,title,author_name,first_publish_year,cover_i", "limit": 12},
                      headers={"User-Agent": USER_AGENT}, timeout=timeout)
        r.raise_for_status()
        libros = []
        for d in r.json().get("docs", []):
            if not d.get("title") or not d.get("author_name"):
                continue
            libros.append({
                "titulo": d["title"],
                "autor": ", ".join(d["author_name"][:2]),
                "anio": d.get("first_publish_year"),
                "portada": f"https://covers.openlibrary.org/b/id/{d['cover_i']}-M.jpg" if d.get("cover_i") else None,
                "url": f"{OPEN_LIBRARY_URL}{d['key']}" if d.get("key") else f"{OPEN_LIBRARY_URL}/search?q={quote_plus(d['title'])}",
                "fuente": "Open Library",
            })
        libros.sort(key=lambda x: x["portada"] is None)   # primero los que tienen portada
        if not libros:
            return _respaldo(destino)[:limite]
        _CACHE[destino] = (time.time(), libros)
        return libros[:limite]
    except Exception:
        return _respaldo(destino)[:limite]
