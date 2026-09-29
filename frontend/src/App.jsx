import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from './api.js'
import Header from './components/Header.jsx'
import Home from './components/Home.jsx'
import Resultados from './components/Resultados.jsx'
import Paquete from './components/Paquete.jsx'
import MisViajes from './components/MisViajes.jsx'
import LoginModal from './components/LoginModal.jsx'
import Toasts from './components/Toasts.jsx'
import { hoyMas } from './data.js'

const leerViajero = () => {
  try { return JSON.parse(localStorage.getItem('th-viajero')) } catch { return null }
}

export default function App() {
  const [vista, setVista] = useState('home')
  const [busqueda, setBusqueda] = useState({ foco: 'PAQUETE', origen: 'AEP', destino: 'BRC', desde: hoyMas(45), hasta: hoyMas(50), pax: 2 })
  const [resultados, setResultados] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [paquete, setPaquete] = useState([])
  const [viajero, setViajero] = useState(leerViajero)
  const [login, setLogin] = useState(false)
  const [toasts, setToasts] = useState([])
  const esperaLogin = useRef(null)

  useEffect(() => {
    if (viajero) localStorage.setItem('th-viajero', JSON.stringify(viajero))
    else localStorage.removeItem('th-viajero')
  }, [viajero])

  const cerrarToast = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), [])
  const avisar = useCallback((texto, tipo = 'info') => {
    const id = Date.now() + Math.random()
    setToasts((t) => [...t, { id, texto, tipo }])
    setTimeout(() => cerrarToast(id), 6000)
  }, [cerrarToast])

  const irA = (v) => { setVista(v); window.scrollTo({ top: 0, behavior: 'smooth' }) }

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

  const alternar = (p) => setPaquete((prev) => prev.some((x) => x.id === p.id)
    ? prev.filter((x) => x.id !== p.id)
    : [...prev.filter((x) => x.tipo !== p.tipo || p.tipo === 'EXCURSION'), p])

  // Devuelve una promesa que se resuelve con el viajero cuando ingresa (o null si cierra el modal)
  const pedirLogin = () => new Promise((resolve) => {
    if (viajero) return resolve(viajero)
    esperaLogin.current = resolve
    setLogin(true)
  })

  const cerrarLogin = useCallback((v = null) => {
    setLogin(false)
    if (v) {
      setViajero(v)
      avisar(`¡Hola, ${v.nombre}!`, 'ok')
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
          <Paquete paquete={paquete} busqueda={busqueda} viajero={viajero} quitar={alternar} irA={irA}
            pedirLogin={pedirLogin} avisar={avisar} onReservado={() => { setPaquete([]); irA('viajes') }} />
        )}
        {vista === 'viajes' && <MisViajes viajero={viajero} pedirLogin={pedirLogin} avisar={avisar} irA={irA} />}
      </main>

      <footer className="pie">
        <div className="contenedor pie-in">
          <span><b>✈ TravelHub</b> · Trabajo Práctico · Desarrollo de Aplicaciones II · UADE</span>
          <span className="gris">Precios en modo demostración. No se realizan cobros reales.</span>
        </div>
      </footer>

      {login && <LoginModal onOk={(v) => cerrarLogin(v)} onClose={() => cerrarLogin(null)} />}
      <Toasts toasts={toasts} cerrar={cerrarToast} />
    </div>
  )
}
