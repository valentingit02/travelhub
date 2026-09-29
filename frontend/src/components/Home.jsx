import SearchBox from './SearchBox.jsx'
import { DESTINOS } from '../data.js'

const BENEFICIOS = [
  ['💡', 'Precios transparentes', 'Te mostramos cada factor que forma el precio final.'],
  ['🧳', 'Armá tu paquete', 'Combiná vuelo, hotel, auto y excursiones y ahorrá un 7 %.'],
  ['🤖', 'Recomendaciones con IA', 'Excursiones sugeridas según tus gustos y el clima.'],
  ['🔒', 'Reserva protegida', 'Si algo falla, cancelamos todo automáticamente.']
]

export default function Home({ busqueda, onBuscar }) {
  return (
    <>
      <section className="hero">
        <div className="contenedor">
          <h1>Tu próximo viaje empieza acá</h1>
          <p className="hero-sub">Vuelos, alojamientos, autos y excursiones en un solo lugar. Buscá sin registrarte.</p>
          <SearchBox inicial={busqueda} onBuscar={onBuscar} />
        </div>
      </section>

      <section className="contenedor beneficios">
        {BENEFICIOS.map(([icono, titulo, texto]) => (
          <div key={titulo} className="beneficio">
            <span className="beneficio-icono">{icono}</span>
            <div><b>{titulo}</b><p>{texto}</p></div>
          </div>
        ))}
      </section>

      <section className="contenedor">
        <div className="seccion-titulo">
          <h2>Destinos que te van a enamorar</h2>
          <p>Elegí uno y te mostramos todo lo disponible para tus fechas.</p>
        </div>
        <div className="destinos">
          {Object.entries(DESTINOS).map(([k, d], i) => (
            <button key={k} className={`destino ${i === 0 ? 'destacado' : ''}`} style={{ background: d.grad }}
              onClick={() => onBuscar({ ...busqueda, destino: k })}>
              <span className="destino-emoji">{d.emoji}</span>
              <span className="destino-info">
                <small>{d.pais}</small>
                <b>{d.nombre}</b>
                <span>{d.tag}</span>
              </span>
              <span className="destino-cta">Ver ofertas →</span>
            </button>
          ))}
        </div>
      </section>

      <section className="contenedor">
        <div className="promo">
          <div>
            <span className="promo-tag">PAQUETES</span>
            <h3>Combiná 2 o más productos y pagá 7 % menos</h3>
            <p>Reservá con anticipación y sumá hasta un 15 % extra de descuento.</p>
          </div>
          <button className="btn btn-acento" onClick={() => onBuscar(busqueda)}>Armar mi paquete</button>
        </div>
      </section>
    </>
  )
}
