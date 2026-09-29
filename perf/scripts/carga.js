// Escenario CARGA: 50 usuarios virtuales + revision de seguimiento cada 5 s.
// SLO (ver perf/SLO.md): p95 <= 500 ms por endpoint (revisar <= 1000 ms), errores < 1 %.
import { sleep } from 'k6';
import { iteracionDeUsuario, revisarSeguimiento, resumenMarkdown } from './lib.js';

export const options = {
  scenarios: {
    usuarios: {
      executor: 'ramping-vus',
      exec: 'usuario',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '60s', target: 50 },
        { duration: '30s', target: 0 },
      ],
    },
    seguimiento: {
      executor: 'constant-arrival-rate',
      exec: 'seguimiento',
      rate: 1,
      timeUnit: '5s',
      duration: '2m',
      preAllocatedVUs: 1,
    },
  },
  thresholds: {
    'http_req_duration{name:POST /incidencias}': ['p(95)<=500'],
    'http_req_duration{name:POST /incidencias/qr}': ['p(95)<=500'],
    'http_req_duration{name:GET /incidencias/:id}': ['p(95)<=500'],
    'http_req_duration{name:POST /seguimiento/revisar}': ['p(95)<=1000'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
};

export function usuario() {
  iteracionDeUsuario();
  sleep(0.2);
}

export function seguimiento() {
  revisarSeguimiento();
}

export function handleSummary(data) {
  return { 'perf/reports/carga.md': resumenMarkdown('carga', data) };
}
