import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { DESTINOS, TIPOS, ciudad, dinero, fecha, fechaHora } from '../data.js'
import KitViaje from './KitViaje.jsx'

const ESTADOS = {
  PENDIENTE: 'Esperando pago',
  PAGADA: 'Pagada',
  CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada',
  FALLIDA: 'No se pudo completar'
}
const PARTICIPANTE = {
  PENDIENTE: 'no pagó', PROCESANDO: 'procesando', PAGADO: 'pagó', RECHAZADO: 'rechazado',
  REEMBOLSADO: 'reembolsado', CANCELADO: 'cancelado'
}

export default function MisViajes({ viajero, credito, pedirLogin, avisar, irA }) {
  const [lista, setLista] = useState(null)
  const [kit, setKit] = useState(null)

  useEffect(() => {
    if (!viajero) return
    let vivo = true
    const cargar = () => api.reservas(viajero.id).then((r) => vivo && setLista(r)).catch(() => {})
    cargar()
    const t = setInterval(cargar, 2000) // los pagos son asincronicos: se actualiza solo
    return () => { vivo = false; clearInterval(t) }
  }, [viajero])

  if (!viajero) {
    return (
      <div className="contenedor">
        <div className="vacio grande">
          <b>Ingresá para ver tus viajes</b>
          <p>Vas a poder seguir cada reserva en tiempo real.</p>
          <button className="btn btn-primario" onClick={pedirLogin}>Ingresar</button>
        </div>
      </div>
    )
  }

  const cancelar = async (id) => {
    if (!confirm('¿Seguro que querés cancelar esta reserva? Se devuelve lo pagado.')) return
    try {
      await api.cancelar(id)
      avisar('Reserva cancelada. Liberamos todo y devolvemos lo pagado.', 'ok')
      setLista(await api.reservas(viajero.id))
    } catch (e) {
      avisar(e.message, 'error')
    }
  }

  const copiar = async (token) => {
    const link = `${window.location.origin}/?pagar=${token}`
    try { await navigator.clipboard.writeText(link); avisar('Link copiado. Mandáselo por WhatsApp.', 'ok') } catch { prompt('Copiá el link:', link) }
  }

  const saldo = Number(credito?.saldo || 0)

  return (
    <div className="contenedor">
      <div className="seccion-titulo izq">
        <span className="sobretitulo oscuro">Tu cuenta</span>
        <h2>Mis viajes</h2>
        <p className="gris">Se actualiza solo: los pagos se procesan de forma asincrónica.</p>
      </div>

      {credito && (saldo > 0 || credito.movimientos?.length > 0) && (
        <div className="credito">
          <div><span className="sobretitulo oscuro">Protección de precio</span>
            <b>{dinero(saldo, 'USD', 2)} de crédito a favor</b>
            <small>Se aplica solo en tu próxima reserva individual.</small></div>
          <ul>{credito.movimientos.slice(0, 4).map((m) => (
            <li key={m.id}><span>{m.motivo}</span><b className={Number(m.monto) >= 0 ? 'pos' : 'neg'}>{Number(m.monto) >= 0 ? '+' : ''}{dinero(m.monto, m.moneda, 2)}</b></li>
          ))}</ul>
        </div>
      )}

      {lista === null && <div className="card-res esqueleto" />}
      {lista?.length === 0 && (
        <div className="vacio grande">
          <b>Todavía no tenés viajes</b>
          <p>Cuando reserves un paquete lo vas a ver acá.</p>
          <button className="btn btn-primario" onClick={() => irA('home')}>Buscar mi próximo viaje</button>
        </div>
      )}
      {lista?.map((r) => {
        const d = DESTINOS[r.destino]
        const pagaron = r.participantes.filter((p) => p.estado === 'PAGADO').length
        const propio = r.participantes.find((p) => p.organizador)
        return (
          <article key={r.id} className="viaje">
            <div className="viaje-banda" style={{ '--c': d?.color, '--t': d?.tono }}>
              <span className="viaje-codigo">{r.destino}</span>
              <div>
                <small>Reserva #{r.id}{r.compartida ? ' · viaje compartido' : ''}</small>
                <h3>{ciudad(r.destino)}</h3>
                <p>{fecha(r.desde)} → {fecha(r.hasta)} · {r.pasajeros} viajero{r.pasajeros > 1 ? 's' : ''}</p>
              </div>
              <span className={`estado ${r.estado}`}>{ESTADOS[r.estado] || r.estado}</span>
            </div>
            <div className="viaje-cuerpo">
              <div>
                <ul className="viaje-items">
                  {r.items.map((i) => (
                    <li key={i.id}>
                      <span className="mini-icono" style={{ background: TIPOS[i.tipo]?.color }}>{TIPOS[i.tipo]?.icono}</span>
                      <span className="viaje-item-nombre">{i.descripcion}</span>
                      <span className={`mini-estado ${i.estado}`}>{i.estado.toLowerCase()}</span>
                      <span className="num">{i.precioCotizado != null ? dinero(i.precioCotizado, r.moneda, 2) : ''}</span>
                    </li>
                  ))}
                </ul>
                {r.motivo && <div className="motivo">{r.motivo}</div>}

                {r.compartida && (
                  <div className="participantes">
                    <div className="progreso">
                      <div className="progreso-barra"><span style={{ width: `${Math.round((pagaron / r.participantes.length) * 100)}%` }} /></div>
                      <small>{pagaron} de {r.participantes.length} pagaron{r.estado === 'PENDIENTE' ? ` · vence ${fechaHora(r.venceEn)}` : ''}</small>
                    </div>
                    {r.participantes.map((p) => (
                      <div key={p.id} className="participante">
                        <span>{p.organizador ? 'Vos' : (p.nombre || p.email)}</span>
                        <span className="num">{dinero(p.monto, r.moneda, 2)}</span>
                        <span className={`mini-estado ${p.estado}`}>{PARTICIPANTE[p.estado]}</span>
                        {r.estado === 'PENDIENTE' && !p.organizador && ['PENDIENTE', 'RECHAZADO'].includes(p.estado) &&
                          <button className="link" onClick={() => copiar(p.token)}>Copiar link</button>}
                      </div>
                    ))}
                  </div>
                )}
                {r.estado === 'PENDIENTE' && propio?.estado === 'RECHAZADO' && (
                  <button className="btn btn-acento btn-chico" onClick={() => { window.location.search = `?pagar=${propio.token}` }}>Reintentar mi pago</button>
                )}

                <div className="viaje-total">
                  <span>Total</span><b>{dinero(r.total, r.moneda, 2)}</b>
                  {Number(r.creditoAplicado) > 0 && <small className="gris">({dinero(r.creditoAplicado, r.moneda, 2)} con crédito)</small>}
                  {['PENDIENTE', 'CONFIRMADA'].includes(r.estado) &&
                    <button className="btn btn-linea btn-chico" onClick={() => cancelar(r.id)}>Cancelar reserva</button>}
                </div>
              </div>
              <details className="saga">
                <summary>Seguimiento · {r.saga.length} pasos</summary>
                <ol className="linea-tiempo">
                  {r.saga.map((s, k) => (
                    <li key={k} className={s.estado}>
                      <b>{s.paso.replaceAll('_', ' ').toLowerCase()}</b>
                      <small>{new Date(s.fecha).toLocaleTimeString('es-AR')} · {s.detalle}</small>
                    </li>
                  ))}
                </ol>
              </details>
            </div>
            {r.estado === 'CONFIRMADA' && (
              <div className="viaje-kit">
                <button className="btn btn-linea btn-chico" onClick={() => setKit(kit === r.id ? null : r.id)}>
                  {kit === r.id ? 'Ocultar kit de viaje' : 'Ver kit de viaje (qué llevar, itinerario y libros)'}
                </button>
                {kit === r.id && <KitViaje reserva={r} preferencias={viajero.preferencias} />}
              </div>
            )}
          </article>
        )
      })}
    </div>
  )
}
