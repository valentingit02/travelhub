"""ia-service: componente de IA de TravelHub (FastAPI)."""
import logging
import time
from contextlib import asynccontextmanager
from datetime import date
from typing import List, Optional

from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field

from . import asistente, db, kit, libros, recomendaciones, resumen
from .anomalias import detector
from .clima import clima
from .config import RABBIT_HABILITADO

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s [%(name)s] %(message)s")


@asynccontextmanager
async def lifespan(app: FastAPI):
    if RABBIT_HABILITADO:
        from .mensajeria import iniciar_consumidor, reentrenar
        reentrenar()          # arranca con lo aprendido en ejecuciones anteriores
        iniciar_consumidor()
    yield


app = FastAPI(title="TravelHub IA", version="3.0",
              description="Anomalias de precio, resumen del viaje, recomendaciones, asistente conversacional "
                          "y kit de viaje (equipaje, itinerario y libros).",
              lifespan=lifespan)


class EvaluarRequest(BaseModel):
    tipoProducto: str = Field(examples=["AUTO"])
    precio: float = Field(gt=0, examples=[1.0])
    moneda: str = "USD"
    destino: Optional[str] = Field(default=None, examples=["BRC"])
    precioAnterior: Optional[float] = Field(default=None, gt=0, examples=[120.0])
    productoRef: Optional[str] = None


class Item(BaseModel):
    tipo: str
    descripcion: str
    precio: Optional[float] = None


class ResumenRequest(BaseModel):
    nombre: str
    destino: str
    desde: date
    hasta: date
    pasajeros: int = Field(ge=1, le=9)
    items: List[Item]
    total: float
    moneda: str


class AsistenteRequest(BaseModel):
    mensaje: str = Field(min_length=1, max_length=500, examples=["Quiero ir a la nieve en julio con mi novia"])
    contexto: Optional[dict] = Field(default=None, description="La 'intencion' devuelta en la respuesta anterior")


@app.get("/health")
def health():
    return {"status": "UP", "modelo": detector.version}


# ---------------------------------------------------------------- anomalias

@app.post("/api/ia/anomalias/evaluar", tags=["Anomalias"])
def evaluar(req: EvaluarRequest):
    inicio = time.perf_counter()
    r = detector.evaluar(req.tipoProducto, req.precio, req.destino, req.precioAnterior)
    db.guardar(req.productoRef, req.tipoProducto, req.precio, req.moneda, r, req.destino, req.precioAnterior)
    return {"esAnomalia": r.es_anomalia, "score": round(r.score, 4), "z": round(r.z, 2), "motivo": r.motivo,
            "fuente": r.fuente, "modeloVersion": r.modelo_version,
            "latenciaMs": round((time.perf_counter() - inicio) * 1000, 2)}


@app.get("/api/ia/anomalias/historial", tags=["Anomalias"])
def historial(limite: int = 50):
    return db.historial(limite)


@app.get("/api/ia/anomalias/modelo", tags=["Anomalias"])
def modelo():
    return detector.resumen()


@app.post("/api/ia/anomalias/reentrenar", tags=["Anomalias"])
def reentrenar():
    grupos = detector.entrenar_con_historial(db.observaciones())
    return {"gruposEntrenados": grupos, "modelo": detector.resumen()}


# ---------------------------------------------------------------- viaje

@app.post("/api/ia/resumen", tags=["Resumen"])
def generar_resumen(req: ResumenRequest):
    datos = req.model_dump()
    datos["desde"], datos["hasta"] = req.desde.isoformat(), req.hasta.isoformat()
    return resumen.generar(datos, clima(req.destino, req.desde, req.hasta))


@app.get("/api/ia/recomendaciones", tags=["Recomendaciones"])
def recomendar(destino: str, desde: date, hasta: date,
               preferencias: str = Query("", description="Separadas por coma: aventura,gastronomia")):
    c = clima(destino, desde, hasta)
    return {"destino": destino.upper(), "clima": c,
            "recomendaciones": recomendaciones.recomendar(destino, preferencias.split(","), c)}


@app.post("/api/ia/asistente", tags=["Asistente"])
def conversar(req: AsistenteRequest):
    """Asistente conversacional: interpreta el pedido y arma un paquete real (catalogo + precios)."""
    inicio = time.perf_counter()
    r = asistente.responder(req.mensaje, req.contexto)
    r["latenciaMs"] = int((time.perf_counter() - inicio) * 1000)
    return r


@app.get("/api/ia/kit", tags=["Kit de viaje"])
def kit_de_viaje(destino: str, desde: date, hasta: date, pax: int = Query(2, ge=1, le=9),
                 preferencias: str = Query("", description="Separadas por coma")):
    """Que llevar segun el clima, itinerario dia por dia y libros para el viaje (Open Library)."""
    if hasta <= desde:
        raise HTTPException(400, "La vuelta tiene que ser posterior a la ida")
    return kit.generar(destino, desde, hasta, pax, preferencias.split(","))


@app.get("/api/ia/libros", tags=["Kit de viaje"])
def libros_para(destino: str, limite: int = Query(5, ge=1, le=10)):
    return libros.sugerir(destino, limite)
