"""Recomendacion de excursiones por contenido: preferencias del viajero + clima del destino."""
from typing import List, Optional

# Catalogo propio de excursiones (mismo que el mock del catalogo) con atributos para el filtrado
EXCURSIONES = {
    "BRC": [("Cerro Catedral", "aventura", True), ("Circuito Chico", "naturaleza", True),
            ("Ruta del chocolate", "gastronomia", False), ("Museo de la Patagonia", "cultura", False)],
    "MDZ": [("Bodegas de Lujan de Cuyo", "gastronomia", False), ("Alta Montaña y Aconcagua", "naturaleza", True),
            ("Rafting en Potrerillos", "aventura", True), ("Museo del Area Fundacional", "cultura", False)],
    "IGR": [("Cataratas lado argentino", "naturaleza", True), ("Gran Aventura en lancha", "aventura", True),
            ("Museo Guemes y selva", "cultura", False)],
    "USH": [("Tren del Fin del Mundo", "cultura", False), ("Navegacion Canal Beagle", "naturaleza", True),
            ("Trekking Laguna Esmeralda", "aventura", True), ("Museo Maritimo", "cultura", False)],
    "MAD": [("Museo del Prado", "cultura", False), ("Tour de tapas", "gastronomia", False),
            ("Toledo dia completo", "cultura", True), ("Parque del Retiro en bici", "naturaleza", True)],
}


def recomendar(destino: str, preferencias: List[str], clima: Optional[dict], top: int = 3) -> List[dict]:
    prefs = {p.strip().lower() for p in preferencias if p.strip()}
    lluvioso = bool(clima and clima["dias_lluvia"] > clima["dias"] / 3)
    frio = bool(clima and clima["max"] < 8)
    resultado = []
    for nombre, categoria, al_aire_libre in EXCURSIONES.get(destino.upper(), []):
        puntaje, motivos = 1.0, []
        if categoria in prefs:
            puntaje += 2.0
            motivos.append(f"te interesa {categoria}")
        if al_aire_libre and (lluvioso or frio):
            puntaje -= 1.0
            motivos.append("al aire libre con clima desfavorable")
        if not al_aire_libre and (lluvioso or frio):
            puntaje += 0.5
            motivos.append("buena opcion bajo techo")
        resultado.append({"nombre": nombre, "categoria": categoria, "alAireLibre": al_aire_libre,
                          "puntaje": round(puntaje, 2), "motivo": ", ".join(motivos) or "popular en el destino"})
    return sorted(resultado, key=lambda r: r["puntaje"], reverse=True)[:top]
