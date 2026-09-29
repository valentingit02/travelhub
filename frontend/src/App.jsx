import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from './api.js'
import Header from './components/Header.jsx'
import Home from './components/Home.jsx'
import Resultados from './components/Resultados.jsx'
import Paquete from './components/Paquete.jsx'
import MisViajes from './components/MisViajes.jsx'
import PagarParte from './components/PagarParte.jsx'
import Asistente from './components/Asistente.jsx'
import LoginModal from './components/LoginModal.jsx'
import Toasts from './components/Toasts.jsx'
import { hoyMas } from './data.js'

const leerViajero = () => {
  try { return JSON.parse(localStorage.getItem('th-viajero')) } catch { return null }
}
const tokenInicial = () => {
  try { return new URLSearchParams(window.location.search).get('pagar') } catch { return null }
}

export default function App() {
  const [token, setToken] = useState(tokenInicial)
  const [vista, setVista] = useState(token ? 'pagar' : 'home')
  const [busqueda, setBusqueda] = useState({ foco: 'PAQUETE', origen: 'AEP', destino: 'BRC', desde: hoyMas(45), hasta: hoyMas(50), pax: 2 })
  const [resultados, setResultados] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [paquete, setPaquete] = useState([])
  const [viajero, setViajero] = useState(leerViajero)
  const [credito, setCredito] = useState(null)
  const [login, setLogin] = useState(false)
  const [toasts, setToasts] = useState([])
  const esperaLogin = useRef(null)

  useEffect(() => {
    if (viajero) localStorage.setItem('th-viajero', JSON.stringify(viajero))
    else localStorage.removeItem('th-viajero')
  }, [viajero])

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

  const cerrarLogin = useCallback((v = null) => {
    setLogin(false)
    if (v) {
      setViajero(v)
      avisar(`Hola, ${v.nombre}.`, 'ok')
    }
    esperaLogin.current?.(v)
    esperaLogin.current = null
  }, [avisar])

  return (
    <div className="app">
      <Header vista={vista} irA={irA} viajero={viajero} cantidad={paquete.length}
        onLogin={() => pedirLogin()} onLogout={() => { setViajero(null); avisar('Cerraste sesión') }} />

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
        {vista === 'pagar' && token && <PagarParte token={token} avisar={avisar} irA={irA} />}
      </main>

      <footer className="pie">
        <div className="contenedor pie-in">
          <span><b>TravelHub</b> · Desarrollo de Aplicaciones II · UADE</span>
          <span className="gris">Modo demostración. No se realizan cobros reales.</span>
        </div>
      </footer>

      {vista !== 'pagar' && <Asistente onUsar={usarSugerencia} onVerOpciones={(b) => buscar({ ...busqueda, ...b, foco: 'PAQUETE' })} />}
      {login && <LoginModal onOk={(v) => cerrarLogin(v)} onClose={() => cerrarLogin(null)} />}
      <Toasts toasts={toasts} cerrar={cerrarToast} />
    </div>
  )
}
