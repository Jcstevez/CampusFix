// Escenario BASELINE: 1 usuario virtual, 30 s. Mide la latencia "sin presion".
// SLO (ver perf/SLO.md): p95 <= 100 ms por endpoint, errores = 0 %.
import { sleep } from 'k6';
import { iteracionDeUsuario, resumenMarkdown } from './lib.js';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    'http_req_duration{name:POST /incidencias}': ['p(95)<=100'],
    'http_req_duration{name:POST /incidencias/qr}': ['p(95)<=100'],
    'http_req_duration{name:GET /incidencias/:id}': ['p(95)<=100'],
    http_req_failed: ['rate==0'],
    checks: ['rate==1'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
};

export default function () {
  iteracionDeUsuario();
  sleep(0.2);
}

export function handleSummary(data) {
  return { 'perf/reports/baseline.md': resumenMarkdown('baseline', data) };
}
