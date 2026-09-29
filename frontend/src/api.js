// Todas las llamadas pasan por el gateway (mismo origen): no hace falta CORS.
async function req(method, url, body) {
  const res = await fetch(url, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    const detalle = data?.detalles?.length ? ` (${data.detalles.join(', ')})` : ''
    throw new Error((data?.mensaje || `Error ${res.status}`) + detalle)
  }
  return data
}

export const api = {
  buscar: (p) => req('GET', `/api/catalogo/buscar?${new URLSearchParams(p)}`),
  cotizarPaquete: (items) => req('POST', '/api/precios/cotizar-paquete', { items }),
  viajeros: (email) => req('GET', `/api/viajeros${email ? `?email=${encodeURIComponent(email)}` : ''}`),
  crearViajero: (v) => req('POST', '/api/viajeros', v),
  reservar: (r) => req('POST', '/api/reservas', r),
  reservas: (viajeroId) => req('GET', `/api/reservas${viajeroId ? `?viajeroId=${viajeroId}` : ''}`),
  cancelar: (id) => req('DELETE', `/api/reservas/${id}`),
  recomendaciones: (p) => req('GET', `/api/ia/recomendaciones?${new URLSearchParams(p)}`),
  seguir: (s) => req('POST', '/api/seguimientos', s)
}
