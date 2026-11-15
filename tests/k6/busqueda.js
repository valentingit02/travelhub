// Prueba de carga basica:  k6 run tests/k6/busqueda.js
// Con Docker (sin instalar k6):
//   docker run --rm -v ./tests/k6:/k6 -e GW=http://host.docker.internal:8080 grafana/k6:0.54.0 run /k6/busqueda.js
//
// RNF: con 50 usuarios simultaneos, p95 < 500 ms y menos de 1 % de errores.
// La carga sube de a poco (como en un uso real) para que la JVM termine de calentar (JIT) antes del pico.
import http from 'k6/http';
import { check, sleep } from 'k6';

const GW = __ENV.GW || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '20s', target: 50 },   // subida
    { duration: '1m', target: 50 },    // 50 usuarios sostenidos
    { duration: '10s', target: 0 },    // bajada
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const destinos = ['BRC', 'MDZ', 'IGR', 'USH'];

export default function () {
  const d = destinos[Math.floor(Math.random() * destinos.length)];
  const r = http.get(`${GW}/api/catalogo/buscar?origen=AEP&destino=${d}&desde=2027-07-10&hasta=2027-07-15&pax=2`);
  check(r, { 'status 200': (res) => res.status === 200 });
  sleep(1);
}
