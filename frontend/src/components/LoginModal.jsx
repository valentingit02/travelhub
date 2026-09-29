import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { PREFERENCIAS } from '../data.js'

export default function LoginModal({ onOk, onClose }) {
  const [modo, setModo] = useState('ingresar')
  const [form, setForm] = useState({ nombre: '', apellido: '', email: '', documento: '', fechaNacimiento: '1995-01-01' })
  const [prefs, setPrefs] = useState(['aventura', 'gastronomia'])
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    const esc = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', esc)
    return () => window.removeEventListener('keydown', esc)
  }, [onClose])

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const ingresar = async (e) => {
    e.preventDefault()
    setError(''); setCargando(true)
    try {
      const [v] = await api.viajeros(form.email.trim())
      if (v) return onOk(v)
      setModo('registrar')
      setError('No encontramos una cuenta con ese email. Completá tus datos para crearla.')
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  const registrar = async (e) => {
    e.preventDefault()
    setError(''); setCargando(true)
    try {
      onOk(await api.crearViajero({ ...form, email: form.email.trim(), preferencias: prefs.join(',') }))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="modal-fondo" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true">
        <button className="modal-cerrar" onClick={onClose} aria-label="Cerrar">×</button>
        <span className="sobretitulo oscuro">TravelHub</span>
        <h3>{modo === 'ingresar' ? 'Ingresá para reservar' : 'Creá tu cuenta'}</h3>
        <p className="gris">Buscar es libre. Para reservar necesitamos saber quién viaja.</p>
        <div className="modal-tabs">
          <button className={modo === 'ingresar' ? 'on' : ''} onClick={() => setModo('ingresar')}>Ingresar</button>
          <button className={modo === 'registrar' ? 'on' : ''} onClick={() => setModo('registrar')}>Crear cuenta</button>
        </div>

        {modo === 'ingresar' ? (
          <form onSubmit={ingresar} className="form">
            <label>Email<input type="email" required autoFocus value={form.email} onChange={set('email')} placeholder="tu@email.com" /></label>
            <button className="btn btn-primario btn-bloque" disabled={cargando}>{cargando ? 'Buscando…' : 'Continuar'}</button>
            <small className="gris">Demo académica: no se pide contraseña.</small>
          </form>
        ) : (
          <form onSubmit={registrar} className="form">
            <div className="form-2">
              <label>Nombre<input required value={form.nombre} onChange={set('nombre')} /></label>
              <label>Apellido<input required value={form.apellido} onChange={set('apellido')} /></label>
            </div>
            <label>Email<input type="email" required value={form.email} onChange={set('email')} /></label>
            <div className="form-2">
              <label>Documento<input required minLength={6} maxLength={20} value={form.documento} onChange={set('documento')} /></label>
              <label>Nacimiento<input type="date" required value={form.fechaNacimiento} onChange={set('fechaNacimiento')} /></label>
            </div>
            <div className="prefs">
              <span>¿Qué te gusta hacer cuando viajás?</span>
              <div>
                {PREFERENCIAS.map((p) => (
                  <button type="button" key={p} className={`chip-toggle ${prefs.includes(p) ? 'on' : ''}`}
                    onClick={() => setPrefs((x) => x.includes(p) ? x.filter((y) => y !== p) : [...x, p])}>{p}</button>
                ))}
              </div>
            </div>
            <button className="btn btn-primario btn-bloque" disabled={cargando}>{cargando ? 'Creando…' : 'Crear cuenta'}</button>
            <small className="gris">Tip de demo: un email que contenga "rechazo" simula una tarjeta denegada.</small>
          </form>
        )}
        {error && <div className="form-error">{error}</div>}
      </div>
    </div>
  )
}
