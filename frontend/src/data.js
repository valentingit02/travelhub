export const DESTINOS = {
  BRC: { nombre: 'Bariloche', pais: 'Argentina', region: 'Patagonia', tag: 'Nieve, lagos y chocolate', color: '#1F4E79', tono: '#9CC3E6' },
  MDZ: { nombre: 'Mendoza', pais: 'Argentina', region: 'Cuyo', tag: 'Vinos y alta montaña', color: '#7A2E3B', tono: '#E8B4A0' },
  IGR: { nombre: 'Puerto Iguazú', pais: 'Argentina', region: 'Litoral', tag: 'Cataratas y selva', color: '#1E5B45', tono: '#A8D5BA' },
  USH: { nombre: 'Ushuaia', pais: 'Argentina', region: 'Tierra del Fuego', tag: 'El fin del mundo', color: '#2F3E4E', tono: '#C9D6E3' },
  MAD: { nombre: 'Madrid', pais: 'España', region: 'Europa', tag: 'Arte, tapas y noches largas', color: '#8A3B12', tono: '#F2C57C' }
}

export const ORIGENES = {
  AEP: 'Buenos Aires · Aeroparque',
  EZE: 'Buenos Aires · Ezeiza',
  COR: 'Córdoba',
  ROS: 'Rosario'
}

export const TIPOS = {
  VUELO: { icono: '✈', clave: 'vuelos', plural: 'Vuelos', singular: 'vuelo', unidad: 'por persona', color: '#1F4FD8' },
  HOTEL: { icono: '⌂', clave: 'hoteles', plural: 'Alojamientos', singular: 'alojamiento', unidad: 'por noche', color: '#6B3FA0' },
  EXCURSION: { icono: '⛰', clave: 'excursiones', plural: 'Excursiones', singular: 'excursión', unidad: 'por persona', color: '#0E7C5A' },
  AUTO: { icono: '⛟', clave: 'autos', plural: 'Autos', singular: 'auto', unidad: 'por día', color: '#C2410C' }
}

export const FOCOS = [
  { id: 'PAQUETE', label: 'Paquetes' },
  { id: 'VUELO', label: 'Vuelos' },
  { id: 'HOTEL', label: 'Alojamientos' },
  { id: 'AUTO', label: 'Autos' },
  { id: 'EXCURSION', label: 'Excursiones' }
]

export const PREFERENCIAS = ['aventura', 'naturaleza', 'gastronomia', 'cultura']

export const hoyMas = (d) => {
  const f = new Date(Date.now() + d * 864e5)
  return new Date(f.getTime() - f.getTimezoneOffset() * 6e4).toISOString().slice(0, 10)
}

export const noches = (desde, hasta) =>
  Math.max(1, Math.round((new Date(hasta) - new Date(desde)) / 864e5))

export const unidades = (tipo, b) => (tipo === 'HOTEL' || tipo === 'AUTO' ? noches(b.desde, b.hasta) : Number(b.pax))

export const estimado = (p, b) => Number(p.precioBase) * unidades(p.tipo, b)

export const dinero = (v, moneda = 'USD', dec = 0) => {
  try {
    return new Intl.NumberFormat('es-AR', { style: 'currency', currency: moneda || 'USD', minimumFractionDigits: dec, maximumFractionDigits: dec }).format(Number(v) || 0)
  } catch {
    return `${moneda} ${Number(v).toFixed(dec)}`
  }
}

export const fecha = (iso) =>
  new Date(`${iso}T00:00:00`).toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short' })

export const fechaHora = (iso) =>
  new Date(iso).toLocaleString('es-AR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })

export const ciudad = (iata) => DESTINOS[iata]?.nombre || iata

export const DETALLE_OCULTO = new Set(['nota', 'rateType', 'precioOriginal', 'co2Kg', 'distanciaKm'])
