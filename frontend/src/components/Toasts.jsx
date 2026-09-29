export default function Toasts({ toasts, cerrar }) {
  return (
    <div className="toasts">
      {toasts.map((t) => (
        <div key={t.id} className={`toast ${t.tipo}`}>
          <span>{t.tipo === 'error' ? '⚠️' : t.tipo === 'ok' ? '✅' : 'ℹ️'}</span>
          <p>{t.texto}</p>
          <button onClick={() => cerrar(t.id)} aria-label="Cerrar">×</button>
        </div>
      ))}
    </div>
  )
}
