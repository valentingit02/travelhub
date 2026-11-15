// Todas las llamadas pasan por el gateway (mismo origen): no hace falta CORS.

// Sesión: el token (JWT) que devuelve /api/auth/* viaja en el header Authorization.
let token = null
let alVencer = () => {}
export const sesion = {
  usar: (t) => { token = t || null },
  // App registra qué hacer si el servidor dice que la sesión ya no vale (401 con token)
  alVencer: (fn) => { alVencer = fn }
}

async function req(method, url, body, headers = {}) {
  const conToken = !!token && !url.startsWith('/api/auth/')
  let res
  try {
    res = await fetch(url, {
      method,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(conToken ? { Authorization: `Bearer ${token}` } : {}),
        ...headers
      },
      body: body ? JSON.stringify(body) : undefined
    })
  } catch {
    throw new Error('No hay conexión con el servidor. ¿Está corriendo docker compose?')
  }
  const text = await res.text()
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { data = null }
  if (res.status === 401 && conToken) alVencer()
  if (!res.ok) {
    const detalle = data?.detalles?.length ? ` (${data.detalles.join(', ')})` : ''
    const error = new Error((data?.mensaje || data?.detail || `Error ${res.status} en ${url.split('?')[0]}`) + detalle)
    error.status = res.status
    throw error
  }
  return data
}

export const nuevaClave = () =>
  (globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`)

export const api = {
  buscar: ({ origen, destino, desde, hasta, pax }) =>
    req('GET', `/api/catalogo/buscar?${new URLSearchParams({ origen, destino, desde, hasta, pax })}`),
  cotizarPaquete: (items) => req('POST', '/api/precios/cotizar-paquete', { items }),
  login: (email, password) => req('POST', '/api/auth/login', { email, password }),
  registro: (v) => req('POST', '/api/auth/registro', v),
  activar: (email, documento, password) => req('POST', '/api/auth/activar', { email, documento, password }),
  yo: () => req('GET', '/api/viajeros/me'),
  actualizarPerfil: (p) => req('PUT', '/api/viajeros/me', p),
  cambiarPassword: (actual, nueva) => req('PUT', '/api/viajeros/me/password', { actual, nueva }),
  // Solo se mandan los ids: el precio lo verifica el servidor contra el catálogo
  reservar: (r, clave) => req('POST', '/api/reservas', r, { 'Idempotency-Key': clave }),
  reservas: (viajeroId) => req('GET', `/api/reservas?viajeroId=${viajeroId}`),
  cancelar: (id) => req('DELETE', `/api/reservas/${id}`),
  creditos: (viajeroId) => req('GET', `/api/viajeros/${viajeroId}/creditos`),
  invitacion: (token) => req('GET', `/api/reservas/compartidas/${encodeURIComponent(token)}`),
  pagarParte: (token, nombre) => req('POST', `/api/reservas/compartidas/${encodeURIComponent(token)}/pagar`, { nombre }),
  recomendaciones: (p) => req('GET', `/api/ia/recomendaciones?${new URLSearchParams(p)}`),
  asistente: (mensaje, contexto) => req('POST', '/api/ia/asistente', { mensaje, contexto }),
  kit: (p) => req('GET', `/api/ia/kit?${new URLSearchParams(p)}`),
  seguir: (s) => req('POST', '/api/seguimientos', s)
}
