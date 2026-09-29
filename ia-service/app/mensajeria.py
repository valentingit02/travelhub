"""
Consumidor asincronico: escucha PrecioCambiado en RabbitMQ, evalua el precio (con su precio
anterior y su destino) y, si es anomalo, publica AnomaliaDetectada. El catalogo deja el auto
EN REVISION o bloquea la oferta del proveedor. Cada REENTRENAR_CADA precios normales, el
modelo se reentrena con el historial real. Se reconecta solo si RabbitMQ se cae.
"""
import json
import logging
import threading
import time

from . import db
from .anomalias import detector
from .config import (COLA_PRECIOS, DLX, EXCHANGE, RABBIT_HOST, RK_ANOMALIA, RK_PRECIO_CAMBIADO)

log = logging.getLogger("ia.mensajeria")
REENTRENAR_CADA = 25
_normales = 0


def reentrenar() -> int:
    grupos = detector.entrenar_con_historial(db.observaciones())
    log.info("Modelo reentrenado con historial real: %s grupos (%s)", grupos, detector.version)
    return grupos


def procesar(evento: dict):
    """Logica pura (testeable sin RabbitMQ). Devuelve el evento a publicar o None."""
    global _normales
    precio = float(evento["precioNuevo"])
    anterior = evento.get("precioAnterior")
    anterior = float(anterior) if anterior is not None else None
    tipo = evento["tipoProducto"]
    destino = evento.get("destino")
    resultado = detector.evaluar(tipo, precio, destino, anterior)
    db.guardar(evento.get("productoRef"), tipo, precio, evento.get("moneda"), resultado, destino, anterior)
    log.info("Evaluado %s %s %.2f (antes %s) -> anomalia=%s [%s] %s", evento.get("productoRef"), tipo, precio,
             anterior, resultado.es_anomalia, resultado.fuente, resultado.motivo)
    if not resultado.es_anomalia:
        _normales += 1
        if _normales % REENTRENAR_CADA == 0:
            reentrenar()
        return None
    return {"productoRef": evento.get("productoRef"), "tipoProducto": tipo, "destino": destino,
            "precio": precio, "moneda": evento.get("moneda"), "score": resultado.score, "motivo": resultado.motivo}


def _consumir():
    import pika
    params = pika.ConnectionParameters(host=RABBIT_HOST, heartbeat=30)
    while True:
        try:
            con = pika.BlockingConnection(params)
            ch = con.channel()
            ch.exchange_declare(EXCHANGE, exchange_type="topic", durable=True)
            # Mismos argumentos que las colas Java: los mensajes rechazados van a la DLQ
            ch.queue_declare(COLA_PRECIOS, durable=True,
                             arguments={"x-dead-letter-exchange": DLX, "x-dead-letter-routing-key": "dlq"})
            ch.queue_bind(COLA_PRECIOS, EXCHANGE, routing_key=RK_PRECIO_CAMBIADO)
            ch.basic_qos(prefetch_count=10)

            def on_message(canal, metodo, props, cuerpo):
                try:
                    salida = procesar(json.loads(cuerpo))
                    if salida:
                        headers = {}
                        if props.headers and "X-Correlation-Id" in props.headers:
                            headers["X-Correlation-Id"] = props.headers["X-Correlation-Id"]
                        canal.basic_publish(EXCHANGE, RK_ANOMALIA, json.dumps(salida),
                                            pika.BasicProperties(content_type="application/json",
                                                                 delivery_mode=2, headers=headers))
                        log.warning("Publicado %s para %s", RK_ANOMALIA, salida["productoRef"])
                    canal.basic_ack(metodo.delivery_tag)
                except Exception as e:
                    log.error("Mensaje invalido, va a la DLQ: %s", e)
                    canal.basic_nack(metodo.delivery_tag, requeue=False)

            ch.basic_consume(COLA_PRECIOS, on_message)
            log.info("Escuchando %s en %s", RK_PRECIO_CAMBIADO, COLA_PRECIOS)
            ch.start_consuming()
        except Exception as e:
            log.warning("RabbitMQ no disponible (%s). Reintento en 5 s", e)
            time.sleep(5)


def iniciar_consumidor():
    hilo = threading.Thread(target=_consumir, name="consumidor-precios", daemon=True)
    hilo.start()
    return hilo
