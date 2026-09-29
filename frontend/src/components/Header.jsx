export default function Header({ vista, irA, viajero, cantidad, onLogin, onLogout }) {
  const link = (id, texto, extra) => (
    <button className={`nav-link ${vista === id ? 'activo' : ''}`} onClick={() => irA(id)}>
      {texto}{extra}
    </button>
  )
  return (
    <header className="topbar">
      <div className="contenedor topbar-in">
        <button className="logo" onClick={() => irA('home')}>
          <span className="logo-icono">✈</span> Travel<b>Hub</b>
        </button>
        <nav className="nav">
          {link('home', 'Inicio')}
          {link('paquete', 'Mi paquete', cantidad > 0 && <span className="badge">{cantidad}</span>)}
          {link('viajes', 'Mis viajes')}
        </nav>
        <div className="usuario">
          {viajero ? (
            <>
              <span className="avatar">{viajero.nombre?.[0]?.toUpperCase()}</span>
              <span className="usuario-nombre">Hola, {viajero.nombre}</span>
              <button className="btn btn-fantasma btn-chico" onClick={onLogout}>Salir</button>
            </>
          ) : (
            <button className="btn btn-claro btn-chico" onClick={onLogin}>👤 Ingresar</button>
          )}
        </div>
      </div>
    </header>
  )
}
