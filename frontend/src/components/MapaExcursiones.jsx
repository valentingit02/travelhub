import { useEffect, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// Mapa del itinerario con OpenStreetMap (gratis, sin API key).
// Pin numerado = excursión del día N · pin "+" = otra excursión recomendada sin día asignado · casita = el destino.

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
const comoLlegar = (lat, lon) => `https://www.google.com/maps/dir/?api=1&destination=${lat},${lon}`
const icono = (clase, texto) => L.divIcon({ className: 'pin-wrap', html: `<span class="pin ${clase}">${texto}</span>`, iconSize: [30, 30], iconAnchor: [15, 15], popupAnchor: [0, -14] })

export default function MapaExcursiones({ mapa, itinerario, seleccionado, onSeleccionar }) {
  const contenedor = useRef(null)
  const mapaRef = useRef(null)
  const marcadores = useRef({})
  const alSeleccionar = useRef(onSeleccionar)
  alSeleccionar.current = onSeleccionar
  const [sinTiles, setSinTiles] = useState(false)

  // Crear el mapa una vez por kit
  useEffect(() => {
    if (!mapa || !contenedor.current) return
    const m = L.map(contenedor.current, { scrollWheelZoom: false })
    let cargadas = 0
    let errores = 0
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">OpenStreetMap</a>'
    })
      .on('tileload', () => { cargadas++; setSinTiles(false) })
      .on('tileerror', () => { errores++; if (errores >= 4 && cargadas === 0) setSinTiles(true) })
      .addTo(m)

    const puntos = [[mapa.centro.lat, mapa.centro.lon]]
    L.marker(puntos[0], { icon: icono('pin-base', '⌂'), title: mapa.ciudad, keyboard: false })
      .bindPopup(`<b>${esc(mapa.ciudad)}</b><br/>Llegada, alojamiento y regreso`)
      .addTo(m)

    itinerario.filter((d) => d.lat != null).forEach((d) => {
      const mk = L.marker([d.lat, d.lon], { icon: icono('', d.dia), title: `Día ${d.dia}: ${d.titulo}`, riseOnHover: true })
        .bindPopup(`<b>Día ${d.dia} · ${esc(d.titulo)}</b><br/><small>${esc(d.detalle)}</small><br/>` +
          `<a href="${comoLlegar(d.lat, d.lon)}" target="_blank" rel="noreferrer">Cómo llegar ↗</a>`)
        .on('click', () => alSeleccionar.current(d.dia))
        .addTo(m)
      marcadores.current[d.dia] = mk
      puntos.push([d.lat, d.lon])
    })

    ;(mapa.otrasExcursiones || []).forEach((e) => {
      L.marker([e.lat, e.lon], { icon: icono('pin-otra', '+'), title: e.nombre })
        .bindPopup(`<b>${esc(e.nombre)}</b><br/><small>También te puede gustar (${esc(e.categoria)}). No entró en el itinerario.</small><br/>` +
          `<a href="${comoLlegar(e.lat, e.lon)}" target="_blank" rel="noreferrer">Cómo llegar ↗</a>`)
        .addTo(m)
      puntos.push([e.lat, e.lon])
    })

    m.fitBounds(L.latLngBounds(puntos).pad(0.2), { maxZoom: 13 })
    mapaRef.current = m
    // El kit se despliega con animación: recalcular el tamaño cuando ya está visible
    const t = setTimeout(() => m.invalidateSize(), 250)
    return () => {
      clearTimeout(t)
      m.remove()
      mapaRef.current = null
      marcadores.current = {}
    }
  }, [mapa, itinerario])

  // Resaltar el día elegido en el itinerario
  useEffect(() => {
    Object.entries(marcadores.current).forEach(([dia, mk]) =>
      mk.getElement()?.querySelector('.pin')?.classList.toggle('activo', Number(dia) === seleccionado))
    const mk = marcadores.current[seleccionado]
    if (mk && mapaRef.current) {
      mapaRef.current.flyTo(mk.getLatLng(), Math.max(mapaRef.current.getZoom(), 12), { duration: 0.6 })
      mk.openPopup()
    }
  }, [seleccionado])

  if (!mapa) return null
  return (
    <div className="mapa-kit">
      <div ref={contenedor} className="mapa-kit-lienzo" role="region" aria-label={`Mapa de excursiones en ${mapa.ciudad}`} />
      {sinTiles && (
        <div className="mapa-kit-aviso">No se pudo cargar el mapa base (¿sin internet?). Los puntos y el itinerario se ven igual.</div>
      )}
      <div className="mapa-kit-leyenda">
        <span><span className="pin mini">1</span> Excursión del día</span>
        <span><span className="pin pin-otra mini">+</span> Otras recomendadas</span>
        <span><span className="pin pin-base mini">⌂</span> {mapa.ciudad}</span>
      </div>
    </div>
  )
}
