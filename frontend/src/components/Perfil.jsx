import { useState } from 'react'
import { api } from '../api.js'
import { ORIGENES, PREFERENCIAS, fechaHora, texto } from '../data.js'

/** Mi perfil: datos personales, preferencias (las usa la IA), aeropuerto habitual y contraseña. */
export default function Perfil({ viajero, onGuardado, pedirLogin, avisar }) {
  if (!viajero) {
    return (
      <div className="contenedor">
        <div className="vacio grande">
          <b>Ingresá para ver tu perfil</b>
          <p>Guardamos tus datos para que reservar sea más rápido.</p>
          <button className="btn btn-primario" onClick={pedirLogin}>Ingresar</button>
        </div>
      </div>
    )
  }
  // key: si cambia el viajero se reinicia el formulario
  return <FormPerfil key={viajero.id} viajero={viajero} onGuardado={onGuardado} avisar={avisar} />
}

function FormPerfil({ viajero, onGuardado, avisar }) {
  const [form, setForm] = useState({
    nombre: viajero.nombre || '',
    apellido: viajero.apellido || '',
    documento: '',
    fechaNacimiento: viajero.fechaNacimiento || '',
    telefono: viajero.telefono || '',
    aeropuertoOrigen: viajero.aeropuertoOrigen || ''
  })
  const [prefs, setPrefs] = useState((viajero.preferencias || '').split(',').filter(Boolean))
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [pw, setPw] = useState({ actual: '', nueva: '', repetir: '' })
  const [errorPw, setErrorPw] = useState('')
  const [cambiando, setCambiando] = useState(false)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })
  const setP = (k) => (e) => setPw({ ...pw, [k]: e.target.value })

  const guardar = async (e) => {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      const v = await api.actualizarPerfil({ ...form, preferencias: prefs.join(',') })
      setForm((f) => ({ ...f, documento: '' }))
      onGuardado(v)
      avisar('Guardamos tu perfil.', 'ok')
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  const cambiarPassword = async (e) => {
    e.preventDefault()
    setErrorPw('')
    if (pw.nueva !== pw.repetir) { setErrorPw('Las contraseñas nuevas no coinciden.'); return }
    setCambiando(true)
    try {
      await api.cambiarPassword(pw.actual, pw.nueva)
      setPw({ actual: '', nueva: '', repetir: '' })
      avisar('Cambiaste tu contraseña.', 'ok')
    } catch (err) {
      setErrorPw(err.message)
    } finally {
      setCambiando(false)
    }
  }

  return (
    <div className="contenedor perfil">
      <div className="seccion-titulo izq">
        <span className="sobretitulo">Tu cuenta</span>
        <h2>Mi perfil</h2>
        <p className="gris">
          {viajero.email}
          {viajero.ultimoIngreso && <> · Último ingreso: {fechaHora(viajero.ultimoIngreso)}</>}
        </p>
      </div>

      <div className="perfil-grilla">
        <form onSubmit={guardar} className="form perfil-tarjeta">
          <h4>Datos personales</h4>
          <div className="form-2">
            <label>Nombre<input required maxLength={80} value={form.nombre} onChange={set('nombre')} autoComplete="given-name" /></label>
            <label>Apellido<input required maxLength={80} value={form.apellido} onChange={set('apellido')} autoComplete="family-name" /></label>
          </div>
          <div className="form-2">
            <label>Nacimiento<input type="date" required value={form.fechaNacimiento} onChange={set('fechaNacimiento')} /></label>
            <label>Teléfono<input type="tel" maxLength={30} value={form.telefono} onChange={set('telefono')} placeholder="+54 11 5555-5555" autoComplete="tel" /></label>
          </div>
          <label>Salgo habitualmente desde
            <select value={form.aeropuertoOrigen} onChange={set('aeropuertoOrigen')}>
              <option value="">Sin preferencia</option>
              {Object.entries(ORIGENES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
          <div className="prefs">
            <span>¿Qué te gusta hacer cuando viajás? <small className="gris">(lo usa la IA para recomendarte excursiones)</small></span>
            <div>
              {PREFERENCIAS.map((p) => (
                <button type="button" key={p} className={`chip-toggle ${prefs.includes(p) ? 'on' : ''}`}
                  onClick={() => setPrefs((x) => x.includes(p) ? x.filter((y) => y !== p) : [...x, p])}>{texto(p)}</button>
              ))}
            </div>
          </div>
          <label>Documento <small className="gris">(actual: {viajero.documentoEnmascarado} · dejalo vacío para no cambiarlo)</small>
            <input minLength={6} maxLength={20} value={form.documento} onChange={set('documento')} placeholder="Nuevo documento" />
          </label>
          <button className="btn btn-primario btn-bloque" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar cambios'}</button>
          {error && <div className="form-error" role="alert">{error}</div>}
        </form>

        <form onSubmit={cambiarPassword} className="form perfil-tarjeta">
          <h4>Contraseña</h4>
          <label>Contraseña actual<input type="password" required value={pw.actual} onChange={setP('actual')} autoComplete="current-password" /></label>
          <label>Nueva (mín. 8)<input type="password" required minLength={8} maxLength={72} value={pw.nueva} onChange={setP('nueva')} autoComplete="new-password" /></label>
          <label>Repetir nueva<input type="password" required minLength={8} maxLength={72} value={pw.repetir} onChange={setP('repetir')} autoComplete="new-password" /></label>
          <button className="btn btn-linea btn-bloque" disabled={cambiando}>{cambiando ? 'Cambiando…' : 'Cambiar contraseña'}</button>
          {errorPw && <div className="form-error" role="alert">{errorPw}</div>}
          <small className="gris">Tu contraseña no se guarda: se guarda solo su hash (BCrypt), así que nadie, ni siquiera TravelHub, puede leerla.</small>
        </form>
      </div>
    </div>
  )
}
