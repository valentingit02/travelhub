-- Ofertas que el catalogo mostro en una busqueda. Es la fuente de verdad del precio:
-- reservas-service consulta esta tabla y nunca confia en el precio que manda el navegador.
CREATE TABLE oferta_publicada (
    id             VARCHAR(1000)            PRIMARY KEY,
    tipo           VARCHAR(20)              NOT NULL,
    proveedor      VARCHAR(30)              NOT NULL,
    nombre         VARCHAR(200)             NOT NULL,
    destino        VARCHAR(3)               NOT NULL,
    precio         NUMERIC(12, 2)           NOT NULL,
    moneda         VARCHAR(3)               NOT NULL,
    ocupacion      DOUBLE PRECISION         NOT NULL,
    bloqueada      BOOLEAN                  NOT NULL DEFAULT FALSE,
    motivo_bloqueo VARCHAR(200),
    actualizada_en TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_en      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_oferta_bloqueada ON oferta_publicada (bloqueada);
