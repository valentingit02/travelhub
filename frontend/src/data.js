export const DESTINOS = {
  BRC: { nombre: 'Bariloche', pais: 'Argentina', emoji: '🏔️', tag: 'Nieve, lagos y chocolate', grad: 'linear-gradient(135deg,#1e3a8a 0%,#0ea5e9 100%)' },
  MDZ: { nombre: 'Mendoza', pais: 'Argentina', emoji: '🍇', tag: 'Vinos y alta montaña', grad: 'linear-gradient(135deg,#7c2d12 0%,#f59e0b 100%)' },
  IGR: { nombre: 'Puerto Iguazú', pais: 'Argentina', emoji: '💦', tag: 'Cataratas y selva', grad: 'linear-gradient(135deg,#064e3b 0%,#10b981 100%)' },
  USH: { nombre: 'Ushuaia', pais: 'Argentina', emoji: '🐧', tag: 'El fin del mundo', grad: 'linear-gradient(135deg,#0f172a 0%,#475569 100%)' },
  MAD: { nombre: 'Madrid', pais: 'España', emoji: '🏛️', tag: 'Arte, tapas y noches largas', grad: 'linear-gradient(135deg,#9f1239 0%,#fb7185 100%)' }
}

export const ORIGENES = {
  AEP: 'Buenos Aires (Aeroparque)',
  EZE: 'Buenos Aires (Ezeiza)',
  COR: 'Córdoba',
  ROS: 'Rosario'
}

export const TIPOS = {
  VUELO: { icono: '✈️', clave: 'vuelos', plural: 'Vuelos', unidad: 'por persona', color: '#2563eb' },
  HOTEL: { icono: '🏨', clave: 'hoteles', plural: 'Hoteles', unidad: 'por noche', color: '#7c3aed' },
  EXCURSION: { icono: '🥾', clave: 'excursiones', plural: 'Excursiones', unidad: 'por persona', color: '#059669' },
  AUTO: { icono: '🚗', clave: 'autos', plural: 'Autos', unidad: 'por día', color: '#ea580c' }
}

export const FOCOS = [
  { id: 'PAQUETE', icono: '🧳', label: 'Paquetes' },
  { id: 'VUELO', icono: '✈️', label: 'Vuelos' },
  { id: 'HOTEL', icono: '🏨', label: 'Alojamientos' },
  { id: 'AUTO', icono: '🚗', label: 'Autos' },
  { id: 'EXCURSION', icono: '🥾', label: 'Excursiones' }
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

export const ciudad = (iata) => DESTINOS[iata]?.nombre || iata
