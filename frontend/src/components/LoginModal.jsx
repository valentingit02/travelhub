import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { PREFERENCIAS, texto } from '../data.js'

// ingresar: email + contraseña · registrar: cuenta nueva · activar: cuenta anterior al login (crea su contraseña)
const TITULOS = {
  ingresar: 'Ingresá para reservar',
  registrar: 'Creá tu cuenta',
  activar: 'Creá tu contraseña'
}

export default function LoginModal({ onOk, onClose }) {
  const [modo, setModo] = useState('ingresar')
  const [form, setForm] = useState({ nombre: '', apellido: '', email: '', documento: '', fechaNacimiento: '1995-01-01', password: '', repetir: '' })
  const [prefs, setPrefs] = useState(['aventura', 'gastronomia'])
  const [verPassword, setVerPassword] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    const esc = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', esc)
    return () => window.removeEventListener('keydown', esc)
  }, [onClose])

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })
  const cambiarModo = (m) => { setModo(m); setError(''); setAviso('') }

  const enviar = (accion) => async (e) => {
    e.preventDefault()
    setError(''); setCargando(true)
    try {
      onOk(await accion())
    } catch (err) {
      setError(err.message)
      return err
    } finally {
      setCargando(false)
    }
  }

  const ingresar = async (e) => {
    const err = await enviar(() => api.login(form.email.trim(), form.password))(e)
    // 409: la cuenta existe pero se creó antes del login -> crear contraseña con el documento
    if (err?.status === 409) {
      setModo('activar')
      setError('')
      setAviso('Tu cuenta se creó antes de que existiera el ingreso con contraseña. Confirmá tu documento y elegí una.')
    }
  }

  const validarRepetida = () => {
    if (form.password !== form.repetir) { setError('Las contraseñas no coinciden.'); return false }
    return true
  }

  const registrar = (e) => {
    if (!validarRepetida()) { e.preventDefault(); return }
    const { repetir, ...datos } = form
    return enviar(() => api.registro({ ...datos, email: form.email.trim(), preferencias: prefs.join(',') }))(e)
  }

  const activar = (e) => {
    if (!validarRepetida()) { e.preventDefault(); return }
    return enviar(() => api.activar(form.email.trim(), form.documento.trim(), form.password))(e)
  }

  const campoPassword = (k, label, autoComplete) => (
    <label>{label}
      <input type={verPassword ? 'text' : 'password'} required minLength={k === 'password' && modo === 'ingresar' ? 1 : 8} maxLength={72}
        value={form[k]} onChange={set(k)} autoComplete={autoComplete} />
    </label>
  )

  const mostrar = (
    <label className="check-linea">
      <input type="checkbox" checked={verPassword} onChange={(e) => setVerPassword(e.target.checked)} /> Mostrar contraseña
    </label>
  )

  return (
    <div className="modal-fondo" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="login-titulo">
        <button className="modal-cerrar" onClick={onClose} aria-label="Cerrar">×</button>
        <span className="sobretitulo oscuro">TravelHub</span>
        <h3 id="login-titulo">{TITULOS[modo]}</h3>
        <p className="gris">Buscar es libre. Para reservar y ver tus viajes necesitás tu cuenta.</p>
        {modo !== 'activar' && (
          <div className="modal-tabs">
            <button className={modo === 'ingresar' ? 'on' : ''} onClick={() => cambiarModo('ingresar')}>Ingresar</button>
            <button className={modo === 'registrar' ? 'on' : ''} onClick={() => cambiarModo('registrar')}>Crear cuenta</button>
          </div>
        )}
        {aviso && <div className="form-aviso">{aviso}</div>}

        {modo === 'ingresar' && (
          <form onSubmit={ingresar} className="form">
            <label>Email<input type="email" required autoFocus value={form.email} onChange={set('email')} placeholder="tu@email.com" autoComplete="username" /></label>
            {campoPassword('password', 'Contraseña', 'current-password')}
            {mostrar}
            <button className="btn btn-primario btn-bloque" disabled={cargando}>{cargando ? 'Ingresando…' : 'Ingresar'}</button>
            <small className="gris">¿Tu cuenta es de antes del ingreso con contraseña? Ingresá tu email y cualquier contraseña: te vamos a pedir que crees una.</small>
          </form>
        )}

        {modo === 'registrar' && (
          <form onSubmit={registrar} className="form">
            <div className="form-2">
              <label>Nombre<input required maxLength={80} value={form.nombre} onChange={set('nombre')} autoComplete="given-name" /></label>
              <label>Apellido<input required maxLength={80} value={form.apellido} onChange={set('apellido')} autoComplete="family-name" /></label>
            </div>
            <label>Email<input type="email" required maxLength={120} value={form.email} onChange={set('email')} autoComplete="username" /></label>
            <div className="form-2">
              {campoPassword('password', 'Contraseña (mín. 8)', 'new-password')}
              {campoPassword('repetir', 'Repetir contraseña', 'new-password')}
            </div>
            {mostrar}
            <div className="form-2">
              <label>Documento<input required minLength={6} maxLength={20} value={form.documento} onChange={set('documento')} /></label>
              <label>Nacimiento<input type="date" required value={form.fechaNacimiento} onChange={set('fechaNacimiento')} /></label>
            </div>
            <div className="prefs">
              <span>¿Qué te gusta hacer cuando viajás?</span>
              <div>
                {PREFERENCIAS.map((p) => (
                  <button type="button" key={p} className={`chip-toggle ${prefs.includes(p) ? 'on' : ''}`}
                    onClick={() => setPrefs((x) => x.includes(p) ? x.filter((y) => y !== p) : [...x, p])}>{texto(p)}</button>
                ))}
              </div>
            </div>
            <button className="btn btn-primario btn-bloque" disabled={cargando}>{cargando ? 'Creando…' : 'Crear cuenta'}</button>
            <small className="gris">Tip de demo: un email que contenga "rechazo" simula una tarjeta denegada.</small>
          </form>
        )}

        {modo === 'activar' && (
          <form onSubmit={activar} className="form">
            <label>Email<input type="email" required value={form.email} onChange={set('email')} autoComplete="username" /></label>
            <label>Documento con el que te registraste<input required value={form.documento} onChange={set('documento')} autoFocus /></label>
            <div className="form-2">
              {campoPassword('password', 'Nueva contraseña (mín. 8)', 'new-password')}
              {campoPassword('repetir', 'Repetir contraseña', 'new-password')}
            </div>
            {mostrar}
            <button className="btn btn-primario btn-bloque" disabled={cargando}>{cargando ? 'Guardando…' : 'Crear contraseña e ingresar'}</button>
            <button type="button" className="btn-texto" onClick={() => cambiarModo('ingresar')}>Volver</button>
          </form>
        )}
        {error && <div className="form-error" role="alert">{error}</div>}
      </div>
    </div>
  )
}
