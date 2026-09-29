import { useEffect, useState } from 'react'
import { api } from './api.js'

const DESTINOS = { BRC: 'Bariloche', MDZ: 'Mendoza', IGR: 'Puerto Iguazú', USH: 'Ushuaia', MAD: 'Madrid' }
const ICONOS = { VUELO: '✈️', HOTEL: '🏨', EXCURSION: '🥾', AUTO: '🚗' }
const hoyMas = (d) => new Date(Date.now() + d * 864e5).toISOString().slice(0, 10)

export default function App() {
  const [tab, setTab] = useState('buscar')
  const [busqueda, setBusqueda] = useState({ origen: 'AEP', destino: 'BRC', desde: hoyMas(45), hasta: hoyMas(50), pax: 2 })
  const [resultados, setResultados] = useState(null)
  const [paquete, setPaquete] = useState([])
  const [viajero, setViajero] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const ejecutar = async (fn) => {
    setError(''); setCargando(true)
    try { return await fn() } catch (e) { setError(e.message) } finally { setCargando(false) }
  }

  const alternar = (p) => setPaquete((prev) => prev.some((x) => x.id === p.id)
    ? prev.filter((x) => x.id !== p.id)
    : [...prev.filter((x) => x.tipo !== p.tipo || p.tipo === 'EXCURSION'), p])

  return (
    <div className="app">
      <header>
        <h1>🌎 TravelHub</h1>
        <nav>
          {[['buscar', 'Buscar'], ['paquete', `Mi paquete (${paquete.length})`], ['reservas', 'Mis reservas']].map(([k, t]) => (
            <button key={k} className={tab === k ? 'activo' : ''} onClick={() => setTab(k)}>{t}</button>
          ))}
        </nav>
        <Sesion viajero={viajero} setViajero={setViajero} ejecutar={ejecutar} />
      </header>

      {error && <div className="error">⚠️ {error}</div>}
      {cargando && <div className="cargando">Cargando…</div>}

      {tab === 'buscar' && (
        <Buscar busqueda={busqueda} setBusqueda={setBusqueda} resultados={resultados} paquete={paquete}
          alternar={alternar} onBuscar={() => ejecutar(async () => setResultados(await api.buscar(busqueda)))} />
      )}
      {tab === 'paquete' && (
        <Paquete paquete={paquete} busqueda={busqueda} viajero={viajero} ejecutar={ejecutar}
          quitar={alternar} onReservado={() => { setPaquete([]); setTab('reservas') }} />
      )}
      {tab === 'reservas' && <Reservas viajero={viajero} ejecutar={ejecutar} />}
    </div>
  )
}

function Sesion({ viajero, setViajero, ejecutar }) {
  const [form, setForm] = useState({ nombre: '', apellido: '', email: '', documento: '', fechaNacimiento: '1995-01-01', preferencias: 'aventura,gastronomia' })
  const [abierto, setAbierto] = useState(false)
  if (viajero) return <div className="sesion">👤 {viajero.nombre} <button onClick={() => setViajero(null)}>Salir</button></div>

  const entrar = () => ejecutar(async () => {
    const existentes = await api.viajeros(form.email)
    setViajero(existentes[0] || await api.crearViajero(form))
    setAbierto(false)
  })
  return (
    <div className="sesion">
      {!abierto ? <button onClick={() => setAbierto(true)}>Ingresar / Registrarme</button> : (
        <div className="popup">
          {['nombre', 'apellido', 'email', 'documento'].map((k) => (
            <input key={k} placeholder={k} value={form[k]} onChange={(e) => setForm({ ...form, [k]: e.target.value })} />
          ))}
          <label>Nacimiento <input type="date" value={form.fechaNacimiento} onChange={(e) => setForm({ ...form, fechaNacimiento: e.target.value })} /></label>
          <input placeholder="preferencias" value={form.preferencias} onChange={(e) => setForm({ ...form, preferencias: e.target.value })} />
          <small>Tip: un email con "rechazo" simula una tarjeta denegada.</small>
          <button className="primario" onClick={entrar}>Continuar</button>
        </div>
      )}
    </div>
  )
}

function Buscar({ busqueda, setBusqueda, resultados, paquete, alternar, onBuscar }) {
  const set = (k) => (e) => setBusqueda({ ...busqueda, [k]: e.target.value })
  return (
    <section>
      <div className="form-busqueda">
        <label>Origen <input value={busqueda.origen} onChange={set('origen')} maxLength={3} /></label>
        <label>Destino
          <select value={busqueda.destino} onChange={set('destino')}>
            {Object.entries(DESTINOS).map(([k, v]) => <option key={k} value={k}>{v} ({k})</option>)}
          </select>
        </label>
        <label>Desde <input type="date" value={busqueda.desde} onChange={set('desde')} /></label>
        <label>Hasta <input type="date" value={busqueda.hasta} onChange={set('hasta')} /></label>
        <label>Pasajeros <input type="number" min="1" max="9" value={busqueda.pax} onChange={set('pax')} /></label>
        <button className="primario" onClick={onBuscar}>Buscar</button>
      </div>
      {resultados && ['vuelos', 'hoteles', 'excursiones', 'autos'].map((k) => (
        <div key={k}>
          <h3>{k[0].toUpperCase() + k.slice(1)} ({resultados[k].length})</h3>
          <div className="grilla">
            {resultados[k].map((p) => {
              const elegido = paquete.some((x) => x.id === p.id)
              return (
                <div key={p.id} className={`tarjeta ${elegido ? 'elegida' : ''}`} onClick={() => alternar(p)}>
                  <div className="titulo">{ICONOS[p.tipo]} {p.nombre}</div>
                  <div className="precio">{p.precioBase} {p.moneda} <span>/ {p.unidad}</span></div>
                  <div className="meta">{p.proveedor} · ocupación {Math.round(p.ocupacion * 100)}%</div>
                  <div className="meta">{Object.entries(p.detalle || {}).map(([a, b]) => `${a}: ${b}`).join(' · ')}</div>
                  <div className="accion">{elegido ? '✓ En el paquete' : '+ Agregar'}</div>
                </div>
              )
            })}
          </div>
        </div>
      ))}
    </section>
  )
}

function Paquete({ paquete, busqueda, viajero, ejecutar, quitar, onReservado }) {
  const [cotizacion, setCotizacion] = useState(null)
  const [recos, setRecos] = useState(null)

  useEffect(() => {
    setCotizacion(null)
    if (!paquete.length) return
    ejecutar(async () => setCotizacion(await api.cotizarPaquete(paquete.map((p) => ({
      tipoProducto: p.tipo, precioBase: p.precioBase, moneda: p.moneda, fechaInicio: busqueda.desde,
      fechaFin: busqueda.hasta, pasajeros: Number(busqueda.pax), ocupacion: p.ocupacion
    })))))
  }, [paquete])

  useEffect(() => {
    api.recomendaciones({ destino: busqueda.destino, desde: busqueda.desde, hasta: busqueda.hasta,
      preferencias: viajero?.preferencias || '' }).then(setRecos).catch(() => setRecos(null))
  }, [busqueda.destino, viajero])

  const reservar = () => ejecutar(async () => {
    if (!viajero) throw new Error('Ingresá o registrate arriba a la derecha para reservar')
    await api.reservar({
      viajeroId: viajero.id, destino: busqueda.destino, desde: busqueda.desde, hasta: busqueda.hasta,
      pasajeros: Number(busqueda.pax),
      items: paquete.map((p) => ({ tipo: p.tipo, productoId: p.id, descripcion: p.nombre, precioBase: p.precioBase, moneda: p.moneda, ocupacion: p.ocupacion }))
    })
    onReservado()
  })

  const seguir = () => ejecutar(async () => {
    if (!viajero) throw new Error('Ingresá para crear una alerta')
    const monto = prompt('Avisarme cuando un auto en este destino cueste menos de (USD por día):', '60')
    if (!monto) return
    await api.seguir({ viajeroId: viajero.id, destino: busqueda.destino, tipoProducto: 'AUTO', precioObjetivo: Number(monto), moneda: 'USD' })
    alert('¡Listo! Te vamos a avisar por mail.')
  })

  if (!paquete.length) return <section><p>Todavía no agregaste nada. Buscá y elegí productos.</p></section>
  return (
    <section className="dos-columnas">
      <div>
        <h3>Tu paquete a {DESTINOS[busqueda.destino]} ({busqueda.desde} → {busqueda.hasta}, {busqueda.pax} pax)</h3>
        {cotizacion && paquete.map((p, i) => {
          const c = cotizacion.items[i]
          return (
            <div key={p.id} className="linea">
              <div><b>{ICONOS[p.tipo]} {p.nombre}</b> <button className="chico" onClick={() => quitar(p)}>quitar</button></div>
              <div className="meta">{c.precioBase} × {c.unidades} {c.unidad} = {c.subtotal}</div>
              {c.factores.map((f) => <div key={f.estrategia} className="factor">× {f.factor} — {f.motivo}</div>)}
              <div className="subtotal">{c.precioFinal} {c.moneda}</div>
            </div>
          )
        })}
        {cotizacion && (
          <div className="total">
            <div>Subtotal: {cotizacion.subtotal} {cotizacion.moneda}</div>
            <div>{cotizacion.motivoDescuento} (× {cotizacion.factorPaquete})</div>
            <div className="grande">Total: {cotizacion.total} {cotizacion.moneda}</div>
            <button className="primario" onClick={reservar}>Reservar y pagar</button>
            <button onClick={seguir}>🔔 Alerta de precio de autos</button>
          </div>
        )}
      </div>
      <aside>
        <h3>🤖 Recomendado para vos</h3>
        {recos?.clima && <p className="meta">Clima ({recos.clima.fuente}): {recos.clima.min}° a {recos.clima.max}°, {recos.clima.dias_lluvia} días de lluvia</p>}
        {recos?.recomendaciones?.map((r) => <div key={r.nombre} className="reco"><b>{r.nombre}</b><div className="meta">{r.motivo}</div></div>)}
        {!recos && <p className="meta">El servicio de IA no está disponible.</p>}
      </aside>
    </section>
  )
}

function Reservas({ viajero, ejecutar }) {
  const [lista, setLista] = useState([])
  const cargar = () => api.reservas(viajero?.id).then(setLista).catch(() => {})

  useEffect(() => {
    cargar()
    const t = setInterval(cargar, 2000) // el pago es asincronico: se refresca solo
    return () => clearInterval(t)
  }, [viajero])

  return (
    <section>
      {!lista.length && <p>No hay reservas todavía.</p>}
      {lista.map((r) => (
        <div key={r.id} className="reserva">
          <div className="cabecera">
            <b>Reserva #{r.id} · {DESTINOS[r.destino] || r.destino} · {r.desde} → {r.hasta}</b>
            <span className={`estado ${r.estado}`}>{r.estado}</span>
          </div>
          <div>Total: {r.total} {r.moneda} {r.motivo && <span className="meta">— {r.motivo}</span>}</div>
          <ul>{r.items.map((i) => <li key={i.id}>{ICONOS[i.tipo]} {i.descripcion} · {i.estado} {i.refExterna && `(${i.refExterna})`}</li>)}</ul>
          <details>
            <summary>Bitácora de la Saga ({r.saga.length} pasos)</summary>
            <table><tbody>{r.saga.map((s, k) => (
              <tr key={k}><td>{new Date(s.fecha).toLocaleTimeString()}</td><td>{s.paso}</td><td className={s.estado}>{s.estado}</td><td>{s.detalle}</td></tr>
            ))}</tbody></table>
          </details>
          {['PENDIENTE', 'CONFIRMADA'].includes(r.estado) &&
            <button onClick={() => ejecutar(async () => { await api.cancelar(r.id); cargar() })}>Cancelar reserva</button>}
        </div>
      ))}
    </section>
  )
}
