"""Recomendacion de excursiones por contenido: preferencias del viajero + clima del destino."""
from typing import List, Optional

# Nombre para mostrar de cada categoria (la clave queda sin tildes porque viaja en la API)
CATEGORIAS = {"aventura": "aventura", "naturaleza": "naturaleza", "gastronomia": "gastronomía", "cultura": "cultura"}

# Catalogo propio de excursiones (mismo que el mock del catalogo) con atributos para el filtrado
EXCURSIONES = {
    "BRC": [("Cerro Catedral", "aventura", True), ("Circuito Chico", "naturaleza", True),
            ("Ruta del chocolate", "gastronomia", False), ("Museo de la Patagonia", "cultura", False)],
    "MDZ": [("Bodegas de Luján de Cuyo", "gastronomia", False), ("Alta Montaña y Aconcagua", "naturaleza", True),
            ("Rafting en Potrerillos", "aventura", True), ("Museo del Área Fundacional", "cultura", False)],
    "IGR": [("Cataratas lado argentino", "naturaleza", True), ("Gran Aventura en lancha", "aventura", True),
            ("Museo Güemes y selva", "cultura", False)],
    "USH": [("Tren del Fin del Mundo", "cultura", False), ("Navegación Canal Beagle", "naturaleza", True),
            ("Trekking Laguna Esmeralda", "aventura", True), ("Museo Marítimo", "cultura", False)],
    "MAD": [("Museo del Prado", "cultura", False), ("Tour de tapas", "gastronomia", False),
            ("Toledo día completo", "cultura", True), ("Parque del Retiro en bici", "naturaleza", True)],
}

# Donde queda cada excursion (lat, lon aproximadas del punto de encuentro) para el mapa del kit de viaje
UBICACIONES = {
    "Cerro Catedral": (-41.1668, -71.4396),
    "Circuito Chico": (-41.0579, -71.5296),
    "Ruta del chocolate": (-41.1338, -71.3082),
    "Museo de la Patagonia": (-41.1336, -71.3104),
    "Bodegas de Luján de Cuyo": (-33.0352, -68.8783),
    "Alta Montaña y Aconcagua": (-32.8236, -69.9381),
    "Rafting en Potrerillos": (-32.9578, -69.1947),
    "Museo del Área Fundacional": (-32.8838, -68.8352),
    "Cataratas lado argentino": (-25.6953, -54.4367),
    "Gran Aventura en lancha": (-25.6868, -54.4428),
    "Museo Güemes y selva": (-25.5985, -54.5662),
    "Tren del Fin del Mundo": (-54.8323, -68.4531),
    "Navegación Canal Beagle": (-54.8072, -68.3021),
    "Trekking Laguna Esmeralda": (-54.6721, -68.1519),
    "Museo Marítimo": (-54.8066, -68.2951),
    "Museo del Prado": (40.4138, -3.6921),
    "Tour de tapas": (40.4115, -3.7090),
    "Toledo día completo": (39.8628, -4.0273),
    "Parque del Retiro en bici": (40.4153, -3.6844),
}


def ubicacion(nombre: str) -> dict:
    """{"lat": .., "lon": ..} o {} si la excursion no tiene ubicacion cargada."""
    if nombre not in UBICACIONES:
        return {}
    lat, lon = UBICACIONES[nombre]
    return {"lat": lat, "lon": lon}


def recomendar(destino: str, preferencias: List[str], clima: Optional[dict], top: int = 3) -> List[dict]:
    prefs = {p.strip().lower() for p in preferencias if p.strip()}
    lluvioso = bool(clima and clima["dias_lluvia"] > clima["dias"] / 3)
    frio = bool(clima and clima["max"] < 8)
    resultado = []
    for nombre, categoria, al_aire_libre in EXCURSIONES.get(destino.upper(), []):
        puntaje, motivos = 1.0, []
        if categoria in prefs:
            puntaje += 2.0
            motivos.append(f"te interesa {CATEGORIAS.get(categoria, categoria)}")
        if al_aire_libre and (lluvioso or frio):
            puntaje -= 1.0
            motivos.append("al aire libre con clima desfavorable")
        if not al_aire_libre and (lluvioso or frio):
            puntaje += 0.5
            motivos.append("buena opcion bajo techo")
        resultado.append({"nombre": nombre, "categoria": categoria, "alAireLibre": al_aire_libre,
                          "puntaje": round(puntaje, 2), "motivo": ", ".join(motivos) or "popular en el destino",
                          **ubicacion(nombre)})
    return sorted(resultado, key=lambda r: r["puntaje"], reverse=True)[:top]
