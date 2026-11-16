// Carga CORTA contra el entorno desplegado (etapa 7 del pipeline).
// Umbrales = perf/SLO.md (escenario "carga"); si se rompen, k6 sale con codigo != 0 y el pipeline falla.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL;

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    'http_req_duration{name:GET /salud}': ['p(95)<500'],
    'http_req_duration{name:GET /}': ['p(95)<500'],
  },
};

export default function () {
  const a = http.get(`${BASE}/salud`, { tags: { name: 'GET /salud' } });
  check(a, { 'salud 200': (r) => r.status === 200 });
  const b = http.get(`${BASE}/`, { tags: { name: 'GET /' } });
  check(b, { 'pantalla 200': (r) => r.status === 200 });
  sleep(0.5);
}
