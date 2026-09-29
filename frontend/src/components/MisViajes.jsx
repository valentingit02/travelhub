import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { DESTINOS, TIPOS, ciudad, dinero, fecha } from '../data.js'

const ESTADOS = {
  PENDIENTE: 'Procesando pago',
  PAGADA: 'Pagada',
  CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada',
  FALLIDA: 'No se pudo completar'
}

export default function MisViajes({ viajero, pedirLogin, avisar, irA }) {
  const [lista, setLista] = useState(null)

  useEffect(() => {
    if (!viajero) return
    let vivo = true
    const cargar = () => api.reservas(viajero.id).then((r) => vivo && setLista(r)).catch(() => {})
    cargar()
    const t = setInterval(cargar, 2000) // el pago es asincronico: se actualiza solo
    return () => { vivo = false; clearInterval(t) }
  }, [viajero])

  if (!viajero) {
    return (
      <div className="contenedor">
        <div className="vacio grande">
          <b>Ingresá para ver tus viajes</b>
          <p>Vas a poder seguir el estado de cada reserva en tiempo real.</p>
          <button className="btn btn-primario" onClick={pedirLogin}>Ingresar</button>
        </div>
      </div>
    )
  }

  const cancelar = async (id) => {
    if (!confirm('¿Seguro que querés cancelar esta reserva?')) return
    try {
      await api.cancelar(id)
      avisar('Reserva cancelada. Liberamos todo en los proveedores.', 'ok')
      setLista(await api.reservas(viajero.id))
    } catch (e) {
      avisar(e.message, 'error')
    }
  }

  return (
    <div className="contenedor">
      <div className="seccion-titulo izq">
        <span className="sobretitulo oscuro">Tu cuenta</span>
        <h2>Mis viajes</h2>
        <p className="gris">Se actualiza solo: el pago se procesa de forma asincrónica.</p>
      </div>
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
        return (
          <article key={r.id} className="viaje">
            <div className="viaje-banda" style={{ '--c': d?.color, '--t': d?.tono }}>
              <span className="viaje-codigo">{r.destino}</span>
              <div>
                <small>Reserva #{r.id}</small>
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
                <div className="viaje-total">
                  <span>Total</span><b>{dinero(r.total, r.moneda, 2)}</b>
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
          </article>
        )
      })}
    </div>
  )
}
