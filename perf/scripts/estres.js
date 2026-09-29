// Escenario ESTRES: sube la carga por escalones para encontrar el punto de quiebre.
// Referencia (ver perf/SLO.md): p95 <= 1000 ms y errores < 5 %.
// Los umbrales NO abortan la prueba: interesa ver en que escalon dejan de cumplirse.
import { sleep } from 'k6';
import { iteracionDeUsuario, resumenMarkdown } from './lib.js';

export const options = {
  stages: [
    { duration: '45s', target: 50 },
    { duration: '45s', target: 100 },
    { duration: '45s', target: 200 },
    { duration: '45s', target: 300 },
    { duration: '45s', target: 400 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    'http_req_duration{name:POST /incidencias}': ['p(95)<=1000'],
    'http_req_duration{name:POST /incidencias/qr}': ['p(95)<=1000'],
    'http_req_duration{name:GET /incidencias/:id}': ['p(95)<=1000'],
    http_req_failed: ['rate<0.05'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max'],
};

export default function () {
  iteracionDeUsuario();
  sleep(0.2);
}

export function handleSummary(data) {
  return { 'perf/reports/estres.md': resumenMarkdown('estres', data) };
}
