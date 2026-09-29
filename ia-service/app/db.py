"""Trazabilidad: guarda cada deteccion en ia_db (tabla deteccion_anomalia). Si la base no esta, se sigue."""
import logging

from .config import DB_DSN

log = logging.getLogger("ia.db")

DDL = """
CREATE TABLE IF NOT EXISTS deteccion_anomalia (
    id SERIAL PRIMARY KEY,
    producto_ref VARCHAR(80),
    tipo_producto VARCHAR(20),
    precio NUMERIC(12,2),
    moneda VARCHAR(3),
    score DOUBLE PRECISION,
    es_anomalia BOOLEAN,
    motivo VARCHAR(200),
    modelo_version VARCHAR(40),
    fecha TIMESTAMP DEFAULT now()
)"""


def guardar(producto_ref, tipo, precio, moneda, resultado) -> None:
    try:
        import psycopg  # import tardio: si no esta instalado, los tests corren igual
        with psycopg.connect(DB_DSN, connect_timeout=2) as con:
            con.execute(DDL)
            con.execute(
                "INSERT INTO deteccion_anomalia (producto_ref, tipo_producto, precio, moneda, score, "
                "es_anomalia, motivo, modelo_version) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)",
                (producto_ref, tipo, precio, moneda, resultado.score, resultado.es_anomalia,
                 resultado.motivo[:200], resultado.modelo_version))
    except Exception as e:
        log.warning("No se pudo guardar la deteccion en ia_db: %s", e)


def historial(limite: int = 50) -> list:
    try:
        import psycopg
        with psycopg.connect(DB_DSN, connect_timeout=2) as con:
            con.execute(DDL)
            filas = con.execute(
                "SELECT producto_ref, tipo_producto, precio, moneda, score, es_anomalia, motivo, "
                "modelo_version, fecha FROM deteccion_anomalia ORDER BY id DESC LIMIT %s", (limite,)).fetchall()
        claves = ["productoRef", "tipoProducto", "precio", "moneda", "score", "esAnomalia", "motivo",
                  "modeloVersion", "fecha"]
        return [dict(zip(claves, [float(v) if k == "precio" else (v.isoformat() if k == "fecha" else v)
                                  for k, v in zip(claves, f)])) for f in filas]
    except Exception as e:
        log.warning("No se pudo leer ia_db: %s", e)
        return []
