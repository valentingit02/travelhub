import { useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import { TIPOS, dinero, fecha } from '../data.js'

const INICIALES = ['Nieve en julio con mi pareja', 'Vinos en Mendoza un fin de semana', 'Cataratas con mi familia', 'Madrid una semana en octubre']

export default function Asistente({ onUsar, onVerOpciones }) {
  const [abierto, setAbierto] = useState(false)
  const [mensajes, setMensajes] = useState([
    { de: 'ia', texto: 'Hola, soy el asistente de TravelHub. Contame qué viaje tenés en mente y te armo un paquete con precios reales.', opciones: INICIALES }
  ])
  const [texto, setTexto] = useState('')
  const [contexto, setContexto] = useState(null)
  const [pensando, setPensando] = useState(false)
  const fin = useRef(null)

  useEffect(() => { fin.current?.scrollIntoView?.({ behavior: 'smooth' }) }, [mensajes, pensando])

  const enviar = async (mensaje) => {
    const m = (mensaje ?? texto).trim()
    if (!m || pensando) return
    setTexto('')
    setMensajes((x) => [...x, { de: 'yo', texto: m }])
    setPensando(true)
    try {
      const r = await api.asistente(m, contexto)
      setContexto(r.intencion)
      setMensajes((x) => [...x, { de: 'ia', texto: r.respuesta, opciones: r.opciones, sugerencia: r.sugerencia, generadoPor: r.generadoPor }])
    } catch (e) {
      setMensajes((x) => [...x, { de: 'ia', texto: `No pude responder: ${e.message}` }])
    } finally {
      setPensando(false)
    }
  }

  const reiniciar = () => {
    setContexto(null)
    setMensajes([{ de: 'ia', texto: 'Empecemos de nuevo. ¿A dónde querés ir?', opciones: INICIALES }])
  }

  if (!abierto) {
    return (
      <button className="asis-lanzador" onClick={() => setAbierto(true)}>
        <span className="etiqueta-ia">IA</span> Planificá con el asistente
      </button>
    )
  }

  return (
    <section className="asis" aria-label="Asistente de viajes">
      <header className="asis-cab">
        <div><span className="etiqueta-ia">IA</span> <b>Asistente de viajes</b></div>
        <div>
          <button className="link claro" onClick={reiniciar}>Reiniciar</button>
          <button className="asis-cerrar" onClick={() => setAbierto(false)} aria-label="Cerrar">×</button>
        </div>
      </header>
      <div className="asis-cuerpo">
        {mensajes.map((m, i) => (
          <div key={i} className={`burbuja ${m.de}`}>
            <p>{m.texto}</p>
            {m.sugerencia && <Sugerencia s={m.sugerencia} onUsar={onUsar} onVerOpciones={onVerOpciones} />}
            {m.opciones?.length > 0 && i === mensajes.length - 1 && (
              <div className="asis-chips">
                {m.opciones.map((o) => <button key={o} onClick={() => enviar(o)}>{o}</button>)}
              </div>
            )}
            {m.generadoPor && <small className="asis-fuente">{m.generadoPor}</small>}
          </div>
        ))}
        {pensando && <div className="burbuja ia"><p className="escribiendo">Buscando disponibilidad y precios…</p></div>}
        <div ref={fin} />
      </div>
      <form className="asis-form" onSubmit={(e) => { e.preventDefault(); enviar() }}>
        <input value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Ej: Bariloche en julio, somos 4, con auto" maxLength={500} />
        <button className="btn btn-acento" disabled={pensando || !texto.trim()}>Enviar</button>
      </form>
    </section>
  )
}

function Sugerencia({ s, onUsar, onVerOpciones }) {
  return (
    <div className="asis-sug">
      <div className="asis-sug-cab">{fecha(s.busqueda.desde)} → {fecha(s.busqueda.hasta)} · {s.busqueda.pax} viajero{s.busqueda.pax > 1 ? 's' : ''}</div>
      {s.productos.map((p) => (
        <div key={p.id} className="asis-sug-item">
          <span className="mini-icono" style={{ background: TIPOS[p.tipo]?.color }}>{TIPOS[p.tipo]?.icono}</span>
          <span>{p.nombre}</span>
        </div>
      ))}
      <div className="asis-sug-total"><span>Total</span><b>{dinero(s.total, s.moneda)}</b></div>
      <div className="asis-sug-acciones">
        <button className="btn btn-primario btn-chico" onClick={() => onUsar(s)}>Usar este paquete</button>
        <button className="btn btn-linea btn-chico" onClick={() => onVerOpciones(s.busqueda)}>Ver más opciones</button>
      </div>
    </div>
  )
}
