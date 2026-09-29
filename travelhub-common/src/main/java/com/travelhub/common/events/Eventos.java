package com.travelhub.common.events;

/** Nombres del exchange y routing keys compartidos por todos los servicios (y por ia-service en Python). */
public final class Eventos {

    private Eventos() { }

    public static final String EXCHANGE = "travelhub.events";
    public static final String DLX = "travelhub.dlx";
    public static final String DLQ = "travelhub.dlq";

    public static final String RESERVA_CREADA = "reserva.creada";
    public static final String RESERVA_CONFIRMADA = "reserva.confirmada";
    public static final String RESERVA_CANCELADA = "reserva.cancelada";
    public static final String PAGO_APROBADO = "pago.aprobado";
    public static final String PAGO_RECHAZADO = "pago.rechazado";
    public static final String PRECIO_CAMBIADO = "precio.cambiado";
    public static final String ANOMALIA_DETECTADA = "anomalia.detectada";
    public static final String ALERTA_PRECIO = "alerta.precio";
}
