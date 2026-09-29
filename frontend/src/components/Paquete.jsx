import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { DESTINOS, TIPOS, ciudad, dinero, fecha, noches } from '../data.js'

export default function Paquete({ paquete, busqueda, viajero, quitar, irA, pedirLogin, avisar, onReservado }) {
  const [cotizacion, setCotizacion] = useState(null)
  const [recos, setRecos] = useState(null)
  const [reservando, setReservando] = useState(false)
  const [objetivo, setObjetivo] = useState('60')

  useEffect(() => {
    let vivo = true
    setCotizacion(null)
    if (!paquete.length) return
    api.cotizarPaquete(paquete.map((p) => ({
      tipoProducto: p.tipo, precioBase: p.precioBase, moneda: p.moneda, fechaInicio: busqueda.desde,
      fechaFin: busqueda.hasta, pasajeros: Number(busqueda.pax), ocupacion: p.ocupacion
    })))
      .then((c) => vivo && setCotizacion(c))
      .catch((e) => vivo && avisar(e.message, 'error'))
    return () => { vivo = false }
  }, [paquete, busqueda])

  useEffect(() => {
    api.recomendaciones({ destino: busqueda.destino, desde: busqueda.desde, hasta: busqueda.hasta, preferencias: viajero?.preferencias || '' })
      .then(setRecos).catch(() => setRecos(null))
  }, [busqueda, viajero])

  const reservar = async () => {
    const v = await pedirLogin()
    if (!v) return
    setReservando(true)
    try {
      await api.reservar({
        viajeroId: v.id, destino: busqueda.destino, desde: busqueda.desde, hasta: busqueda.hasta,
        pasajeros: Number(busqueda.pax),
        items: paquete.map((p) => ({ tipo: p.tipo, productoId: p.id, descripcion: p.nombre, precioBase: p.precioBase, moneda: p.moneda, ocupacion: p.ocupacion }))
      })
      avisar('¡Reserva creada! Estamos procesando tu pago.', 'ok')
      onReservado()
    } catch (e) {
      avisar(e.message, 'error')
    } finally {
      setReservando(false)
    }
  }

  const crearAlerta = async () => {
    const v = await pedirLogin()
    if (!v) return
    try {
      await api.seguir({ viajeroId: v.id, destino: busqueda.destino, tipoProducto: 'AUTO', precioObjetivo: Number(objetivo), moneda: 'USD' })
      avisar(`Te avisamos por mail si un auto en ${ciudad(busqueda.destino)} baja de USD ${objetivo} por día.`, 'ok')
    } catch (e) {
      avisar(e.message, 'error')
    }
  }

  if (!paquete.length) {
    return (
      <div className="contenedor">
        <div className="vacio grande">
          <span>🧳</span>
          <b>Tu paquete está vacío</b>
          <p>Buscá vuelos, alojamientos, autos o excursiones y agregalos acá.</p>
          <button className="btn btn-primario" onClick={() => irA('home')}>Empezar a buscar</button>
        </div>
      </div>
    )
  }

  const destino = DESTINOS[busqueda.destino]
  const faltantes = Object.keys(TIPOS).filter((t) => t !== 'EXCURSION' && !paquete.some((p) => p.tipo === t))
  const ahorro = cotizacion ? Number(cotizacion.subtotal) - Number(cotizacion.total) : 0

  return (
    <div className="contenedor">
      <ol className="pasos">
        <li className="hecho"><span>1</span> Elegí</li>
        <li className="actual"><span>2</span> Revisá tu paquete</li>
        <li><span>3</span> Confirmá y pagá</li>
      </ol>

      <div className="paquete-layout">
        <section>
          <div className="viaje-cabecera" style={{ background: destino?.grad }}>
            <span className="viaje-emoji">{destino?.emoji}</span>
            <div>
              <h2>{ciudad(busqueda.destino)}</h2>
              <p>{fecha(busqueda.desde)} → {fecha(busqueda.hasta)} · {noches(busqueda.desde, busqueda.hasta)} noches · {busqueda.pax} pasajero{busqueda.pax > 1 ? 's' : ''}</p>
            </div>
          </div>

          {paquete.map((p, i) => {
            const c = cotizacion?.items?.[i]
            const tipo = TIPOS[p.tipo]
            return (
              <article key={p.id} className="item-paquete">
                <div className="item-icono" style={{ background: tipo.color }}>{tipo.icono}</div>
                <div className="item-cuerpo">
                  <div className="item-titulo">
                    <b>{p.nombre}</b>
                    <button className="link" onClick={() => quitar(p)}>Quitar</button>
                  </div>
                  {!c && <div className="linea-carga" />}
                  {c && (
                    <>
                      <small className="gris">{dinero(c.precioBase, c.moneda, 2)} × {c.unidades} {c.unidad} = {dinero(c.subtotal, c.moneda, 2)}</small>
                      <div className="factores">
                        {c.factores.map((f) => {
                          const pct = Math.round((Number(f.factor) - 1) * 100)
                          return (
                            <span key={f.estrategia} className={`factor ${pct > 0 ? 'sube' : pct < 0 ? 'baja' : ''}`} title={f.estrategia}>
                              {pct > 0 ? '▲' : pct < 0 ? '▼' : '='} {pct > 0 ? '+' : ''}{pct} % · {f.motivo}
                            </span>
                          )
                        })}
                      </div>
                    </>
                  )}
                </div>
                <div className="item-precio">{c ? dinero(c.precioFinal, c.moneda, 2) : '…'}</div>
              </article>
            )
          })}

          {faltantes.length > 0 && (
            <div className="sugerir">
              <span>¿Te falta algo?</span>
              {faltantes.map((t) => (
                <button key={t} className="btn btn-fantasma-oscuro btn-chico" onClick={() => irA('resultados')}>
                  {TIPOS[t].icono} Agregar {TIPOS[t].plural.toLowerCase().replace(/s$/, '').replace(/e$/, '')}
                </button>
              ))}
            </div>
          )}
        </section>

        <aside className="lateral">
          <div className="resumen">
            <h3>Resumen</h3>
            {!cotizacion && <div className="linea-carga" />}
            {cotizacion && (
              <>
                <div className="fila"><span>Subtotal</span><span>{dinero(cotizacion.subtotal, cotizacion.moneda, 2)}</span></div>
                {ahorro > 0 && <div className="fila verde"><span>{cotizacion.motivoDescuento}</span><span>− {dinero(ahorro, cotizacion.moneda, 2)}</span></div>}
                <div className="fila total"><span>Total</span><span>{dinero(cotizacion.total, cotizacion.moneda, 2)}</span></div>
                <small className="gris">Precio final para {busqueda.pax} pasajero{busqueda.pax > 1 ? 's' : ''}. Impuestos incluidos.</small>
                <button className="btn btn-acento btn-bloque" disabled={reservando} onClick={reservar}>
                  {reservando ? 'Reservando…' : viajero ? 'Reservar y pagar' : 'Ingresar y reservar'}
                </button>
                <div className="seguro">🔒 Si un proveedor falla, cancelamos todo y no se cobra nada.</div>
              </>
            )}
          </div>

          <div className="tarjeta-lateral">
            <h4>🔔 Alerta de precio</h4>
            <p className="gris">Te avisamos por mail si un auto en {ciudad(busqueda.destino)} baja de:</p>
            <div className="alerta-form">
              <span>USD</span>
              <input type="number" min="1" value={objetivo} onChange={(e) => setObjetivo(e.target.value)} />
              <span>/día</span>
              <button className="btn btn-primario btn-chico" onClick={crearAlerta}>Crear</button>
            </div>
          </div>

          <div className="tarjeta-lateral ia">
            <h4>🤖 Recomendado para vos</h4>
            {recos?.clima && (
              <div className="clima">
                <span>{recos.clima.min < 5 ? '❄️' : recos.clima.dias_lluvia > recos.clima.dias / 3 ? '🌧️' : '☀️'}</span>
                <div>
                  <b>{recos.clima.min}° a {recos.clima.max}°</b>
                  <small>{recos.clima.dias_lluvia} días de lluvia · {recos.clima.fuente}</small>
                </div>
              </div>
            )}
            {recos?.recomendaciones?.map((r) => (
              <div key={r.nombre} className="reco">
                <b>{r.nombre}</b>
                <small>{r.categoria} · {r.motivo}</small>
              </div>
            ))}
            {!recos && <p className="gris">El servicio de IA no está disponible en este momento.</p>}
            {!viajero && recos && <small className="gris">Ingresá para recibir recomendaciones según tus gustos.</small>}
          </div>
        </aside>
      </div>
    </div>
  )
}
