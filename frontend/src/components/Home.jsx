import SearchBox from './SearchBox.jsx'
import { DESTINOS } from '../data.js'

const BENEFICIOS = [
  ['01', 'Precio transparente', 'Ves cada factor que forma el precio: temporada, anticipación y ocupación.'],
  ['02', 'Precio garantizado', 'El precio lo verifica el servidor al reservar. Lo que ves es lo que pagás.'],
  ['03', 'Paquete con descuento', 'Combiná dos o más productos y el total baja un 7 %.'],
  ['04', 'Reserva protegida', 'Si un proveedor falla, cancelamos todo automáticamente y no se cobra.']
]

export default function Home({ busqueda, onBuscar }) {
  const destinos = Object.entries(DESTINOS)
  return (
    <>
      <section className="hero">
        <div className="contenedor hero-in">
          <p className="sobretitulo">Vuelos · Alojamientos · Autos · Excursiones</p>
          <h1>Viajá a donde quieras,<br /><span>pagá lo que corresponde.</span></h1>
          <p className="hero-sub">Armá tu viaje en un solo lugar. Buscar no requiere cuenta.</p>
          <SearchBox inicial={busqueda} onBuscar={onBuscar} />
        </div>
      </section>

      <section className="contenedor beneficios">
        {BENEFICIOS.map(([n, titulo, texto]) => (
          <div key={n} className="beneficio">
            <span className="beneficio-num">{n}</span>
            <b>{titulo}</b>
            <p>{texto}</p>
          </div>
        ))}
      </section>

      <section className="contenedor">
        <div className="seccion-titulo">
          <span className="sobretitulo oscuro">Destinos</span>
          <h2>Dónde empezar</h2>
        </div>
        <div className="destinos">
          {destinos.map(([k, d], i) => (
            <button key={k} className={`destino ${i === 0 ? 'destacado' : ''}`}
              style={{ '--c': d.color, '--t': d.tono }} onClick={() => onBuscar({ ...busqueda, destino: k })}>
              <span className="destino-num">{String(i + 1).padStart(2, '0')}</span>
              <span className="destino-codigo">{k}</span>
              <span className="destino-info">
                <small>{d.region} · {d.pais}</small>
                <b>{d.nombre}</b>
                <span>{d.tag}</span>
              </span>
              <span className="destino-cta">Ver disponibilidad →</span>
            </button>
          ))}
        </div>
      </section>

      <section className="contenedor">
        <div className="promo">
          <div>
            <span className="sobretitulo oscuro">Paquetes</span>
            <h3>Combiná y pagá menos</h3>
            <p>7 % off con dos o más productos, y hasta 15 % más si reservás con anticipación.</p>
          </div>
          <button className="btn btn-primario" onClick={() => onBuscar(busqueda)}>Armar mi paquete</button>
        </div>
      </section>
    </>
  )
}
