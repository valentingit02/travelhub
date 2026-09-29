import { useEffect, useState } from 'react'
import { DESTINOS, FOCOS, ORIGENES, hoyMas, noches } from '../data.js'

export default function SearchBox({ inicial, onBuscar, compacto = false }) {
  const [b, setB] = useState(inicial)
  const [error, setError] = useState('')

  useEffect(() => setB(inicial), [inicial])

  const set = (k) => (e) => setB({ ...b, [k]: e.target.value })
  const pax = (d) => setB({ ...b, pax: Math.min(9, Math.max(1, Number(b.pax) + d)) })

  const enviar = (e) => {
    e.preventDefault()
    if (!b.desde || !b.hasta) return setError('Elegí las fechas de ida y vuelta')
    if (b.hasta <= b.desde) return setError('La vuelta tiene que ser posterior a la ida')
    setError('')
    onBuscar({ ...b, pax: Number(b.pax) })
  }

  return (
    <form className={`searchbox ${compacto ? 'compacto' : ''}`} onSubmit={enviar}>
      {!compacto && (
        <div className="sb-tabs" role="tablist">
          {FOCOS.map((f) => (
            <button type="button" role="tab" key={f.id} aria-selected={b.foco === f.id}
              className={b.foco === f.id ? 'on' : ''} onClick={() => setB({ ...b, foco: f.id })}>
              {f.label}
            </button>
          ))}
        </div>
      )}
      <div className="sb-campos">
        <label className="campo">
          <span>Desde</span>
          <select value={b.origen} onChange={set('origen')}>
            {Object.entries(ORIGENES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
        <label className="campo">
          <span>Hacia</span>
          <select value={b.destino} onChange={set('destino')}>
            {Object.entries(DESTINOS).map(([k, d]) => <option key={k} value={k}>{d.nombre}, {d.pais}</option>)}
          </select>
        </label>
        <label className="campo">
          <span>Ida</span>
          <input type="date" min={hoyMas(1)} value={b.desde} onChange={set('desde')} />
        </label>
        <label className="campo">
          <span>Vuelta {b.desde && b.hasta > b.desde && <em>{noches(b.desde, b.hasta)} noches</em>}</span>
          <input type="date" min={b.desde || hoyMas(1)} value={b.hasta} onChange={set('hasta')} />
        </label>
        <div className="campo">
          <span>Viajeros</span>
          <div className="stepper">
            <button type="button" onClick={() => pax(-1)} aria-label="Menos viajeros">−</button>
            <b>{b.pax}</b>
            <button type="button" onClick={() => pax(1)} aria-label="Más viajeros">+</button>
          </div>
        </div>
        <button className="btn btn-acento btn-buscar" type="submit">Buscar</button>
      </div>
      {error && <div className="sb-error">{error}</div>}
    </form>
  )
}
