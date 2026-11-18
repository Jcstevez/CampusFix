// Carga CORTA contra el entorno desplegado (etapa 7b del pipeline).
// Umbrales = SLO del Corte 2 (perf/SLO.md, escenario "carga") sobre los endpoints de los retos.
// Si se rompe un umbral, k6 sale con codigo != 0 y el job "desplegar" falla.
import { iteracionDeUsuario, revisarSeguimiento, resumenMarkdown } from './lib.js';
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL;

export const options = {
  scenarios: {
    // 10 usuarios reportando (texto / QR / consulta) durante 45 s
    usuarios: { executor: 'constant-vus', exec: 'usuario', vus: 10, duration: '45s' },
    // Reto 1: revision de pendientes cada 5 s
    seguimiento: {
      executor: 'constant-arrival-rate', exec: 'seguimiento',
      rate: 1, timeUnit: '5s', duration: '45s', preAllocatedVUs: 1,
    },
    // Disponibilidad: salud y pantalla
    salud: { executor: 'constant-vus', exec: 'salud', vus: 1, duration: '45s' },
  },
  thresholds: {
    'http_req_duration{name:POST /incidencias}': ['p(95)<500'],     // Reto 2
    'http_req_duration{name:POST /incidencias/qr}': ['p(95)<500'],  // Reto 3
    'http_req_duration{name:GET /incidencias/:id}': ['p(95)<500'],  // Reto 1
    'http_req_duration{name:POST /seguimiento/revisar}': ['p(95)<1000'], // Reto 1
    'http_req_duration{name:GET /salud}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
};

export function usuario() { iteracionDeUsuario(); sleep(0.5); }
export function seguimiento() { revisarSeguimiento(); }
export function salud() {
  const r = http.get(`${BASE}/salud`, { tags: { name: 'GET /salud' } });
  check(r, { 'salud 200': (x) => x.status === 200 });
  sleep(1);
}

export function handleSummary(data) {
  return { 'perf/reports/despliegue.md': resumenMarkdown('despliegue', data) };
}
