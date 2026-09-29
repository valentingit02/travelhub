import { useEffect, useState } from 'react'
import { api } from '../api.js'

/** Kit de viaje generado por la IA: qué llevar, itinerario día por día y libros para el viaje. */
export default function KitViaje({ reserva, preferencias }) {
  const [kit, setKit] = useState(null)
  const [error, setError] = useState('')
  const [hechos, setHechos] = useState({})

  useEffect(() => {
    api.kit({ destino: reserva.destino, desde: reserva.desde, hasta: reserva.hasta, pax: reserva.pasajeros, preferencias: preferencias || '' })
      .then(setKit).catch((e) => setError(e.message))
  }, [reserva.id])

  if (error) return <p className="gris">No se pudo generar el kit: {error}</p>
  if (!kit) return <div className="linea-carga" />

  return (
    <div className="kit">
      <div>
        <h5>Qué llevar {kit.clima && <small>· {kit.clima.min}° a {kit.clima.max}°</small>}</h5>
        <ul className="checklist">
          {kit.equipaje.map((e) => (
            <li key={e.item} className={hechos[e.item] ? 'hecho' : ''}>
              <label><input type="checkbox" checked={!!hechos[e.item]} onChange={() => setHechos((h) => ({ ...h, [e.item]: !h[e.item] }))} />
                <span>{e.item}</span></label>
              <small>{e.motivo}</small>
            </li>
          ))}
        </ul>
      </div>
      <div>
        <h5>Itinerario sugerido</h5>
        <ol className="itinerario">
          {kit.itinerario.map((d) => (
            <li key={d.dia}><b>Día {d.dia} · {d.titulo}</b><small>{d.detalle}</small></li>
          ))}
        </ol>
      </div>
      <div>
        <h5>Para leer en el viaje</h5>
        <div className="libros">
          {kit.libros.map((l) => (
            <a key={l.titulo} href={l.url} target="_blank" rel="noreferrer" className="libro">
              {l.portada ? <img src={l.portada} alt="" loading="lazy" /> : <span className="libro-sin">{l.titulo[0]}</span>}
              <span><b>{l.titulo}</b><small>{l.autor}{l.anio ? ` · ${l.anio}` : ''}</small></span>
            </a>
          ))}
        </div>
        <small className="gris">Fuente: {kit.libros[0]?.fuente || 'Open Library'}</small>
      </div>
    </div>
  )
}
