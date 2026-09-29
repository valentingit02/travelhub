import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { DESTINOS, TIPOS, ciudad, dinero, fecha, fechaHora } from '../data.js'

const TEXTO = {
  PENDIENTE: 'Pendiente de pago',
  PROCESANDO: 'Procesando tu pago…',
  PAGADO: 'Tu parte está paga',
  RECHAZADO: 'El pago fue rechazado',
  REEMBOLSADO: 'Te devolvimos lo pagado',
  CANCELADO: 'El viaje fue cancelado'
}

/** Pantalla a la que llega un amigo desde el mail de invitación: paga su parte sin crear cuenta. */
export default function PagarParte({ token, avisar, irA }) {
  const [v, setV] = useState(null)
  const [error, setError] = useState('')
  const [nombre, setNombre] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let vivo = true
    const cargar = () => api.invitacion(token).then((x) => { if (vivo) { setV(x); setError('') } })
      .catch((e) => vivo && setError(e.message))
    cargar()
    const t = setInterval(cargar, 2000)
    return () => { vivo = false; clearInterval(t) }
  }, [token])

  const pagar = async () => {
    setEnviando(true)
    try {
      setV(await api.pagarParte(token, nombre || null))
    } catch (e) {
      avisar(e.message, 'error')
    } finally {
      setEnviando(false)
    }
  }

  if (error) {
    return <div className="contenedor"><div className="vacio grande"><b>No pudimos abrir tu invitación</b><p>{error}</p>
      <button className="btn btn-primario" onClick={() => irA('home')}>Ir a TravelHub</button></div></div>
  }
  if (!v) return <div className="contenedor"><div className="card-res esqueleto" style={{ marginTop: 40 }} /></div>

  const d = DESTINOS[v.destino]
  const puedePagar = v.estadoReserva === 'PENDIENTE' && ['PENDIENTE', 'RECHAZADO'].includes(v.estado)
  const progreso = Math.round((v.pagaron / v.participantes) * 100)

  return (
    <div className="contenedor pagar">
      <div className="viaje-cabecera" style={{ '--c': d?.color, '--t': d?.tono }}>
        <span className="viaje-codigo">{v.destino}</span>
        <div>
          <small>{v.organizador} te invitó a viajar</small>
          <h2>{ciudad(v.destino)}</h2>
          <p>{fecha(v.desde)} → {fecha(v.hasta)} · {v.pasajeros} viajero{v.pasajeros > 1 ? 's' : ''}</p>
        </div>
      </div>
      <div className="pagar-layout">
        <section className="tarjeta-lateral">
          <h4>Qué incluye</h4>
          <ul className="viaje-items">
            {v.items.map((i, k) => (
              <li key={k}><span className="mini-icono" style={{ background: TIPOS[i.tipo]?.color }}>{TIPOS[i.tipo]?.icono}</span>
                <span className="viaje-item-nombre">{i.descripcion}</span><span /><span /></li>
            ))}
          </ul>
          <div className="progreso">
            <div className="progreso-barra"><span style={{ width: `${progreso}%` }} /></div>
            <small>{v.pagaron} de {v.participantes} ya pagaron · la reserva se confirma cuando paguen todos</small>
          </div>
        </section>
        <aside className="resumen">
          <h3>Tu parte</h3>
          <div className="fila total"><span>A pagar</span><span>{dinero(v.monto, v.moneda, 2)}</span></div>
          <div className={`estado-pago ${v.estado}`}>{TEXTO[v.estado] || v.estado}</div>
          {v.motivo && <div className="motivo">{v.motivo}</div>}
          {v.estadoReserva === 'CONFIRMADA' && <div className="ok-caja">¡Pagaron todos! El viaje está confirmado. Te llegó el detalle por mail.</div>}
          {['FALLIDA', 'CANCELADA'].includes(v.estadoReserva) && <div className="motivo">La reserva quedó {v.estadoReserva.toLowerCase()}. Si pagaste, se te devuelve.</div>}
          {puedePagar && (
            <>
              <label className="campo-simple">Tu nombre
                <input value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Opcional" maxLength={120} />
              </label>
              <button className="btn btn-acento btn-bloque" disabled={enviando} onClick={pagar}>
                {enviando ? 'Enviando…' : v.estado === 'RECHAZADO' ? 'Reintentar pago' : `Pagar ${dinero(v.monto, v.moneda, 2)}`}
              </button>
              <small className="gris">Pagá antes del {fechaHora(v.venceEn)}. Si no pagan todos a tiempo, se cancela y se devuelve lo pagado.</small>
            </>
          )}
        </aside>
      </div>
    </div>
  )
}
