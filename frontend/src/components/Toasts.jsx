export default function Toasts({ toasts, cerrar }) {
  return (
    <div className="toasts" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className={`toast ${t.tipo}`}>
          <p>{t.texto}</p>
          {t.accion && <button className="link" onClick={() => { t.accion.fn(); cerrar(t.id) }}>{t.accion.texto}</button>}
          <button className="toast-cerrar" onClick={() => cerrar(t.id)} aria-label="Cerrar">×</button>
        </div>
      ))}
    </div>
  )
}
