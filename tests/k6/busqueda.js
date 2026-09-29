// Prueba de carga basica:  k6 run tests/k6/busqueda.js
// RNF: p95 < 500 ms con cache y menos de 1 % de errores.
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 50,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const destinos = ['BRC', 'MDZ', 'IGR', 'USH'];

export default function () {
  const d = destinos[Math.floor(Math.random() * destinos.length)];
  const r = http.get(`http://localhost:8080/api/catalogo/buscar?origen=AEP&destino=${d}&desde=2027-07-10&hasta=2027-07-15&pax=2`);
  check(r, { 'status 200': (res) => res.status === 200 });
  sleep(1);
}
