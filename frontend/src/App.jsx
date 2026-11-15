import { useCallback, useEffect, useRef, useState } from 'react'
import { api, sesion as sesionApi } from './api.js'
import Header from './components/Header.jsx'
import Home from './components/Home.jsx'
import Resultados from './components/Resultados.jsx'
import Paquete from './components/Paquete.jsx'
import MisViajes from './components/MisViajes.jsx'
import PagarParte from './components/PagarParte.jsx'
import Asistente from './components/Asistente.jsx'
import LoginModal from './components/LoginModal.jsx'
import Perfil from './components/Perfil.jsx'
import Toasts from './components/Toasts.jsx'
import { ORIGENES, hoyMas } from './data.js'

// Sesión guardada: { token, expiraEn, viajero }. Si venció se descarta y hay que ingresar de nuevo.
const leerSesion = () => {
  try {
    localStorage.removeItem('th-viajero') // sesión de la versión sin contraseña: ya no sirve
    const s = JSON.parse(localStorage.getItem('th-sesion'))
    if (!s?.token || !s?.viajero || new Date(s.expiraEn) <= new Date()) return null
    sesionApi.usar(s.token)
    return s
  } catch { return null }
}
const origenDe = (v, actual) => (v?.aeropuertoOrigen && ORIGENES[v.aeropuertoOrigen]) ? v.aeropuertoOrigen : actual
const tokenInicial = () => {
  try { return new URLSearchParams(window.location.search).get('pagar') } catch { return null }
}

export default function App() {
  const [token, setToken] = useState(tokenInicial)
  const [vista, setVista] = useState(token ? 'pagar' : 'home')
  const [sesion, setSesion] = useState(leerSesion)
  const viajero = sesion?.viajero || null
  const [busqueda, setBusqueda] = useState(() => ({ foco: 'PAQUETE', origen: origenDe(sesion?.viajero, 'AEP'), destino: 'BRC', desde: hoyMas(45), hasta: hoyMas(50), pax: 2 }))
  const [resultados, setResultados] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [paquete, setPaquete] = useState([])
  const [credito, setCredito] = useState(null)
  const [login, setLogin] = useState(false)
  const [toasts, setToasts] = useState([])
  const esperaLogin = useRef(null)

  useEffect(() => {
    sesionApi.usar(sesion?.token)
    try {
      if (sesion) localStorage.setItem('th-sesion', JSON.stringify(sesion))
      else localStorage.removeItem('th-sesion')
    } catch { /* sin localStorage: la sesión dura lo que la pestaña */ }
  }, [sesion])

  // Solo cambian los datos del perfil; el token sigue siendo el mismo
  const actualizarViajero = useCallback((v) => {
    setSesion((s) => (s ? { ...s, viajero: v } : s))
    setBusqueda((b) => ({ ...b, origen: origenDe(v, b.origen) }))
  }, [])

  // Al abrir la app con una sesión guardada: se trae el perfil al día (y se valida el token)
  useEffect(() => {
    if (sesion?.token) api.yo().then(actualizarViajero).catch(() => {})
  }, []) // solo al abrir

  const refrescarCredito = useCallback(() => {
    if (!viajero) { setCredito(null); return }
    api.creditos(viajero.id).then(setCredito).catch(() => setCredito(null))
  }, [viajero])
  useEffect(() => { refrescarCredito() }, [refrescarCredito, vista])

  const cerrarToast = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), [])
  const avisar = useCallback((texto, tipo = 'info', accion = null) => {
    const id = Date.now() + Math.random()
    setToasts((t) => [...t, { id, texto, tipo, accion }])
    setTimeout(() => cerrarToast(id), accion ? 10000 : 6000)
  }, [cerrarToast])

  // El servidor rechazó el token (venció o se cambió la clave): se cierra la sesión
  // (varias llamadas pueden fallar juntas: se avisa una sola vez)
  const sesionRef = useRef(sesion)
  useEffect(() => { sesionRef.current = sesion }, [sesion])
  useEffect(() => {
    sesionApi.alVencer(() => {
      if (!sesionRef.current) return
      sesionRef.current = null
      sesionApi.usar(null)
      setSesion(null)
      avisar('Tu sesión venció. Ingresá de nuevo.', 'error')
    })
  }, [avisar])

  const irA = (v) => {
    if (v !== 'pagar' && token) {
      setToken(null)
      window.history.replaceState(null, '', window.location.pathname)
    }
    setVista(v)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Buscar NO requiere cuenta: solo reservar y ver "Mis viajes".
  const buscar = async (b) => {
    setBusqueda(b)
    setPaquete((p) => p.filter((x) => x.destino === b.destino))
    irA('resultados')
    setBuscando(true)
    setResultados(null)
    try {
      setResultados(await api.buscar(b))
    } catch (e) {
      avisar(e.message, 'error')
    } finally {
      setBuscando(false)
    }
  }

  const rebuscar = () => { setPaquete([]); buscar(busqueda) }

  const alternar = (p) => setPaquete((prev) => prev.some((x) => x.id === p.id)
    ? prev.filter((x) => x.id !== p.id)
    : [...prev.filter((x) => x.tipo !== p.tipo || p.tipo === 'EXCURSION'), p])

  // El asistente propone un paquete ya buscado y cotizado: se carga tal cual
  const usarSugerencia = (s) => {
    setBusqueda({ ...busqueda, ...s.busqueda, foco: 'PAQUETE' })
    setPaquete(s.productos)
    irA('paquete')
  }

  const pedirLogin = () => new Promise((resolve) => {
    if (viajero) return resolve(viajero)
    esperaLogin.current = resolve
    setLogin(true)
  })

  // s = { token, expiraEn, viajero } (respuesta de /api/auth/*) o null si cerró el modal
  const cerrarLogin = useCallback((s = null) => {
    setLogin(false)
    const v = s?.viajero || null
    if (s) {
      sesionApi.usar(s.token) // antes del resolve: la acción que esperaba el login ya sale con token
      setSesion(s)
      setBusqueda((b) => ({ ...b, origen: origenDe(v, b.origen) }))
      avisar(`Hola, ${v.nombre}.`, 'ok')
    }
    esperaLogin.current?.(v)
    esperaLogin.current = null
  }, [avisar])

  const salir = () => {
    setSesion(null)
    if (vista === 'perfil') irA('home')
    avisar('Cerraste sesión')
  }

  return (
    <div className="app">
      <Header vista={vista} irA={irA} viajero={viajero} cantidad={paquete.length}
        onLogin={() => pedirLogin()} onLogout={salir} />

      <main>
        {vista === 'home' && <Home busqueda={busqueda} onBuscar={buscar} />}
        {vista === 'resultados' && (
          <Resultados busqueda={busqueda} onBuscar={buscar} resultados={resultados} buscando={buscando}
            paquete={paquete} alternar={alternar} verPaquete={() => irA('paquete')} />
        )}
        {vista === 'paquete' && (
          <Paquete paquete={paquete} busqueda={busqueda} viajero={viajero} credito={credito} quitar={alternar} irA={irA}
            pedirLogin={pedirLogin} avisar={avisar} rebuscar={rebuscar}
            onReservado={() => { setPaquete([]); irA('viajes') }} />
        )}
        {vista === 'viajes' && (
          <MisViajes viajero={viajero} credito={credito} pedirLogin={pedirLogin} avisar={avisar} irA={irA} />
        )}
        {vista === 'perfil' && (
          <Perfil viajero={viajero} onGuardado={actualizarViajero} pedirLogin={pedirLogin} avisar={avisar} />
        )}
        {vista === 'pagar' && token && <PagarParte token={token} avisar={avisar} irA={irA} />}
      </main>

      <footer className="pie">
        <div className="contenedor pie-in">
          <span><b>TravelHub</b> · Desarrollo de Aplicaciones II · UADE</span>
          <span className="gris">Modo demostración. No se realizan cobros reales.</span>
        </div>
      </footer>

      {vista !== 'pagar' && <Asistente onUsar={usarSugerencia} onVerOpciones={(b) => buscar({ ...busqueda, ...b, foco: 'PAQUETE' })} />}
      {login && <LoginModal onOk={(s) => cerrarLogin(s)} onClose={() => cerrarLogin(null)} />}
      <Toasts toasts={toasts} cerrar={cerrarToast} />
    </div>
  )
}
