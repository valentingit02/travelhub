"""
Persistencia de la IA en ia_db:
 - deteccion_anomalia: trazabilidad de cada evaluacion.
 - precio_observado: precios normales reales, con los que se reentrena el modelo.
Si la base no esta disponible, se loguea y se sigue (la IA no se cae).
"""
import logging

from .config import DB_DSN

log = logging.getLogger("ia.db")

DDL = """
CREATE TABLE IF NOT EXISTS deteccion_anomalia (
    id SERIAL PRIMARY KEY,
    producto_ref VARCHAR(1000),
    tipo_producto VARCHAR(20),
    destino VARCHAR(3),
    precio NUMERIC(12,2),
    precio_anterior NUMERIC(12,2),
    moneda VARCHAR(3),
    score DOUBLE PRECISION,
    es_anomalia BOOLEAN,
    motivo VARCHAR(200),
    fuente VARCHAR(40),
    modelo_version VARCHAR(40),
    fecha TIMESTAMP DEFAULT now()
);
ALTER TABLE deteccion_anomalia ALTER COLUMN producto_ref TYPE VARCHAR(1000);
ALTER TABLE deteccion_anomalia ADD COLUMN IF NOT EXISTS destino VARCHAR(3);
ALTER TABLE deteccion_anomalia ADD COLUMN IF NOT EXISTS precio_anterior NUMERIC(12,2);
ALTER TABLE deteccion_anomalia ADD COLUMN IF NOT EXISTS fuente VARCHAR(40);
CREATE TABLE IF NOT EXISTS precio_observado (
    id SERIAL PRIMARY KEY,
    tipo_producto VARCHAR(20) NOT NULL,
    destino VARCHAR(3),
    precio NUMERIC(12,2) NOT NULL,
    fecha TIMESTAMP DEFAULT now()
);
"""


def _conectar():
    import psycopg  # import tardio: si no esta instalado, los tests corren igual
    con = psycopg.connect(DB_DSN, connect_timeout=2, autocommit=True)
    con.execute(DDL)
    return con


def guardar(producto_ref, tipo, precio, moneda, resultado, destino=None, precio_anterior=None) -> None:
    try:
        with _conectar() as con:
            con.execute(
                "INSERT INTO deteccion_anomalia (producto_ref, tipo_producto, destino, precio, precio_anterior, "
                "moneda, score, es_anomalia, motivo, fuente, modelo_version) "
                "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                (producto_ref, tipo, destino, precio, precio_anterior, moneda, resultado.score,
                 resultado.es_anomalia, resultado.motivo[:200], resultado.fuente[:40], resultado.modelo_version))
            if not resultado.es_anomalia:
                con.execute("INSERT INTO precio_observado (tipo_producto, destino, precio) VALUES (%s,%s,%s)",
                            (tipo, destino, precio))
    except Exception as e:
        log.warning("No se pudo guardar en ia_db: %s", e)


def observaciones(limite: int = 20000) -> list:
    try:
        with _conectar() as con:
            filas = con.execute("SELECT tipo_producto, destino, precio FROM precio_observado "
                                "ORDER BY id DESC LIMIT %s", (limite,)).fetchall()
        return [(t, d, float(p)) for t, d, p in filas]
    except Exception as e:
        log.warning("No se pudo leer precio_observado: %s", e)
        return []


def historial(limite: int = 50) -> list:
    try:
        with _conectar() as con:
            filas = con.execute(
                "SELECT producto_ref, tipo_producto, destino, precio, precio_anterior, moneda, score, es_anomalia, "
                "motivo, fuente, modelo_version, fecha FROM deteccion_anomalia ORDER BY id DESC LIMIT %s",
                (limite,)).fetchall()
        claves = ["productoRef", "tipoProducto", "destino", "precio", "precioAnterior", "moneda", "score",
                  "esAnomalia", "motivo", "fuente", "modeloVersion", "fecha"]
        salida = []
        for f in filas:
            d = dict(zip(claves, f))
            for k in ("precio", "precioAnterior"):
                d[k] = float(d[k]) if d[k] is not None else None
            d["fecha"] = d["fecha"].isoformat() if d["fecha"] else None
            salida.append(d)
        return salida
    except Exception as e:
        log.warning("No se pudo leer ia_db: %s", e)
        return []
