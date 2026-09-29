import { useEffect, useMemo, useState } from 'react'
import SearchBox from './SearchBox.jsx'
import { TIPOS, DESTINOS, ciudad, dinero, estimado, fecha, noches, unidades } from '../data.js'

const TAB_POR_FOCO = { PAQUETE: 'vuelos', VUELO: 'vuelos', HOTEL: 'hoteles', AUTO: 'autos', EXCURSION: 'excursiones' }
const ORDEN_TABS = ['vuelos', 'hoteles', 'autos', 'excursiones']
const TIPO_DE_TAB = { vuelos: 'VUELO', hoteles: 'HOTEL', autos: 'AUTO', excursiones: 'EXCURSION' }

export default function Resultados({ busqueda, onBuscar, resultados, buscando, paquete, alternar, verPaquete }) {
  const [tab, setTab] = useState(TAB_POR_FOCO[busqueda.foco] || 'vuelos')
  const [orden, setOrden] = useState('precio')
  const [precioMax, setPrecioMax] = useState(null)
  const [proveedores, setProveedores] = useState([])

  useEffect(() => setTab(TAB_POR_FOCO[busqueda.foco] || 'vuelos'), [busqueda])
  useEffect(() => { setPrecioMax(null); setProveedores([]) }, [tab, resultados])

  const lista = resultados?.[tab] || []
  const rango = useMemo(() => {
    const precios = lista.map((p) => Number(p.precioBase))
    return precios.length ? [Math.floor(Math.min(...precios)), Math.ceil(Math.max(...precios))] : [0, 0]
  }, [lista])
  const todosProveedores = [...new Set(lista.map((p) => p.proveedor))]

  const visibles = lista
    .filter((p) => precioMax == null || Number(p.precioBase) <= precioMax)
    .filter((p) => !proveedores.length || proveedores.includes(p.proveedor))
    .sort((a, b) => orden === 'precio' ? a.precioBase - b.precioBase
      : orden === 'precio-desc' ? b.precioBase - a.precioBase
        : a.ocupacion - b.ocupacion)

  const totalPaquete = paquete.reduce((s, p) => s + estimado(p, busqueda), 0)
  const destino = DESTINOS[busqueda.destino]

  return (
    <div className="resultados">
      <div className="franja-busqueda">
        <div className="contenedor"><SearchBox inicial={busqueda} onBuscar={onBuscar} compacto /></div>
      </div>

      <div className="contenedor">
        <div className="res-cabecera" style={{ background: destino?.grad }}>
          <div>
            <small>{destino?.pais}</small>
            <h2>{destino?.emoji} {ciudad(busqueda.destino)}</h2>
            <p>{fecha(busqueda.desde)} → {fecha(busqueda.hasta)} · {noches(busqueda.desde, busqueda.hasta)} noches · {busqueda.pax} pasajero{busqueda.pax > 1 ? 's' : ''}</p>
          </div>
        </div>

        <div className="tabs">
          {ORDEN_TABS.map((k) => {
            const tipo = TIPOS[TIPO_DE_TAB[k]]
            const items = resultados?.[k] || []
            const min = items.length ? Math.min(...items.map((p) => Number(p.precioBase))) : null
            const elegidos = paquete.filter((p) => p.tipo === TIPO_DE_TAB[k]).length
            return (
              <button key={k} className={`tab ${tab === k ? 'on' : ''}`} onClick={() => setTab(k)}>
                <span className="tab-icono">{tipo.icono}</span>
                <span className="tab-texto">
                  <b>{tipo.plural} {elegidos > 0 && <span className="check">✓</span>}</b>
                  <small>{buscando ? 'Buscando…' : min != null ? `desde ${dinero(min, items[0].moneda)}` : 'Sin resultados'}</small>
                </span>
              </button>
            )
          })}
        </div>

        <div className="res-layout">
          <aside className="filtros">
            <h4>Ordenar por</h4>
            {[['precio', 'Menor precio'], ['precio-desc', 'Mayor precio'], ['disponibilidad', 'Más disponibilidad']].map(([v, t]) => (
              <label key={v} className="radio">
                <input type="radio" name="orden" checked={orden === v} onChange={() => setOrden(v)} /> {t}
              </label>
            ))}
            {rango[1] > rango[0] && (
              <>
                <h4>Precio máximo <span>{dinero(precioMax ?? rango[1], lista[0]?.moneda)}</span></h4>
                <input type="range" min={rango[0]} max={rango[1]} value={precioMax ?? rango[1]}
                  onChange={(e) => setPrecioMax(Number(e.target.value))} />
                <div className="rango-leyenda"><span>{dinero(rango[0], lista[0]?.moneda)}</span><span>{dinero(rango[1], lista[0]?.moneda)}</span></div>
              </>
            )}
            {todosProveedores.length > 1 && (
              <>
                <h4>Proveedor</h4>
                {todosProveedores.map((pr) => (
                  <label key={pr} className="radio">
                    <input type="checkbox" checked={proveedores.includes(pr)}
                      onChange={() => setProveedores((x) => x.includes(pr) ? x.filter((y) => y !== pr) : [...x, pr])} /> {pr}
                  </label>
                ))}
              </>
            )}
            <div className="filtro-nota">💡 Los precios son base. En tu paquete vas a ver el precio final con temporada, anticipación y ocupación.</div>
          </aside>

          <section className="lista">
            {buscando && [1, 2, 3].map((i) => <div key={i} className="card-res esqueleto" />)}
            {!buscando && resultados && !visibles.length && (
              <div className="vacio">
                <span>🔎</span>
                <b>No encontramos {TIPOS[TIPO_DE_TAB[tab]].plural.toLowerCase()} con esos filtros</b>
                <p>Probá cambiar las fechas o quitar filtros.</p>
              </div>
            )}
            {!buscando && visibles.map((p) => (
              <Tarjeta key={p.id} p={p} busqueda={busqueda} elegido={paquete.some((x) => x.id === p.id)} alternar={alternar} />
            ))}
          </section>
        </div>
      </div>

      {paquete.length > 0 && (
        <div className="barra-paquete">
          <div className="contenedor barra-in">
            <div className="barra-iconos">
              {paquete.map((p) => <span key={p.id} title={p.nombre}>{TIPOS[p.tipo].icono}</span>)}
            </div>
            <div className="barra-texto">
              <b>Tu paquete · {paquete.length} producto{paquete.length > 1 ? 's' : ''}</b>
              <small>Total estimado {dinero(totalPaquete, paquete[0].moneda)}{new Set(paquete.map((p) => p.tipo)).size >= 2 ? ' · incluye 7 % off por paquete' : ''}</small>
            </div>
            <button className="btn btn-acento" onClick={verPaquete}>Ver mi paquete →</button>
          </div>
        </div>
      )}
    </div>
  )
}

function Tarjeta({ p, busqueda, elegido, alternar }) {
  const tipo = TIPOS[p.tipo]
  const detalle = Object.entries(p.detalle || {}).filter(([k]) => k !== 'nota' && k !== 'rateType')
  const u = unidades(p.tipo, busqueda)
  const ultimos = p.ocupacion >= 0.75
  const esDemoFalla = p.id === 'MOCK-HOTEL-FALLA'
  return (
    <article className={`card-res ${elegido ? 'elegida' : ''}`}>
      <div className="card-foto" style={{ background: `linear-gradient(135deg, ${tipo.color}, ${tipo.color}99)` }}>
        <span>{tipo.icono}</span>
        {p.tipo === 'HOTEL' && p.detalle?.estrellas && <span className="estrellas">{'★'.repeat(Number(p.detalle.estrellas))}</span>}
      </div>
      <div className="card-info">
        <div className="card-proveedor">{p.proveedor === 'MOCK' ? 'Simulado' : p.proveedor}</div>
        <h3>{p.nombre}</h3>
        <div className="chips">
          {detalle.map(([k, v]) => <span key={k} className="chip">{k}: {v}</span>)}
        </div>
        {p.tipo !== 'EXCURSION' && (
          <div className="ocupacion">
            <div className="ocupacion-barra"><span style={{ width: `${Math.round(p.ocupacion * 100)}%` }} /></div>
            <small className={ultimos ? 'alerta' : ''}>{ultimos ? '🔥 ¡Quedan pocos lugares!' : `${Math.round(p.ocupacion * 100)} % ocupado`}</small>
          </div>
        )}
        {esDemoFalla && <div className="aviso-demo">⚠️ Demo: este hotel siempre rechaza la reserva para mostrar la compensación (Saga).</div>}
      </div>
      <div className="card-precio">
        <small>Precio {tipo.unidad}</small>
        <div className="precio-grande">{dinero(p.precioBase, p.moneda)}</div>
        <small>{u} {p.unidad} · total est. <b>{dinero(estimado(p, busqueda), p.moneda)}</b></small>
        <button className={`btn ${elegido ? 'btn-ok' : 'btn-primario'}`} onClick={() => alternar(p)}>
          {elegido ? '✓ Agregado' : 'Agregar al paquete'}
        </button>
        {elegido && <button className="link" onClick={() => alternar(p)}>Quitar</button>}
      </div>
    </article>
  )
}
