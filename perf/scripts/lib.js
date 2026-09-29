// Utilidades compartidas por los escenarios de carga de CampusFix.
import http from 'k6/http';
import { check } from 'k6';

export const BASE = __ENV.BASE_URL || 'http://localhost:8080';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

const REPORTES = [
  ['Proyector no funciona', 'No enciende el equipo', 'Bloque B - Aula 302'],
  ['Fuga de agua en el baño', 'Se está inundando', 'Bloque B - Piso 1'],
  ['Sin WiFi', 'No hay internet en la sala', 'Bloque A - Aula 101'],
  ['Silla rota', 'Falta una pata', 'Bloque A - Aula 201'],
  ['Pared rayada', '', 'Bloque C - Pasillo'],
  ['Computador no enciende', 'Sale humo', 'Bloque C - Laboratorio 101'],
];

const OBJETOS = ['PROY-B302', 'PC-C101-01', 'WIFI-A2', 'PUERTA-A201', 'LUZ-A201', 'BANO-B1'];

function elegir(lista) {
  return lista[Math.floor(Math.random() * lista.length)];
}

// Reto 2: reporte de texto, el sistema clasifica tipo y prioridad.
export function reportarTexto() {
  const [titulo, descripcion, ubicacion] = elegir(REPORTES);
  const res = http.post(
    `${BASE}/incidencias`,
    JSON.stringify({ titulo, descripcion, ubicacion }),
    { headers: JSON_HEADERS, tags: { name: 'POST /incidencias' } },
  );
  check(res, {
    'POST /incidencias -> 201': (r) => r.status === 201,
    'trae tipo y prioridad': (r) => r.status === 201 && !!r.json('tipo') && !!r.json('prioridad'),
  });
  return res.status === 201 ? res.json('id') : null;
}

// Reto 3: reporte escaneando el QR de un objeto (contenido del QR ya leido).
export function reportarQr() {
  const qr = `campusfix://objeto/${elegir(OBJETOS)}`;
  const res = http.post(
    `${BASE}/incidencias/qr`,
    JSON.stringify({ qr, descripcion: 'Reportado por QR' }),
    { headers: JSON_HEADERS, tags: { name: 'POST /incidencias/qr' } },
  );
  check(res, { 'POST /incidencias/qr -> 201': (r) => r.status === 201 });
  return res.status === 201 ? res.json('id') : null;
}

// Reto 1: consultar el estado de un reporte.
export function consultar(id) {
  const res = http.get(`${BASE}/incidencias/${id}`, {
    tags: { name: 'GET /incidencias/:id' },
  });
  check(res, { 'GET /incidencias/:id -> 200': (r) => r.status === 200 });
}

// Reto 1: revision de pendientes (escala y avisa lo que se esta olvidando).
export function revisarSeguimiento() {
  const res = http.post(`${BASE}/seguimiento/revisar`, null, {
    tags: { name: 'POST /seguimiento/revisar' },
  });
  check(res, { 'POST /seguimiento/revisar -> 200': (r) => r.status === 200 });
}

// Una iteracion de usuario: mezcla 50 % texto / 40 % QR / consulta del reporte creado.
export function iteracionDeUsuario() {
  const suerte = Math.random();
  const id = suerte < 0.5 ? reportarTexto() : reportarQr();
  if (id && suerte < 0.9) {
    consultar(id);
  }
}

// Resumen legible (tabla markdown) que se guarda en perf/reports/<nombre>.md
export function resumenMarkdown(nombre, data) {
  const filas = [];
  for (const [clave, m] of Object.entries(data.metrics)) {
    if (!clave.startsWith('http_req_duration{name:')) continue;
    const v = m.values;
    const nombreEndpoint = clave.slice('http_req_duration{name:'.length, -1);
    filas.push(
      `| ${nombreEndpoint} | ${v.avg.toFixed(1)} | ${v.med.toFixed(1)} | ${v['p(90)'].toFixed(1)} | ${v['p(95)'].toFixed(1)} | ${v.max.toFixed(1)} |`,
    );
  }
  const total = data.metrics.http_reqs ? data.metrics.http_reqs.values : { count: 0, rate: 0 };
  const fallos = data.metrics.http_req_failed ? data.metrics.http_req_failed.values.rate : 0;
  const umbrales = [];
  for (const [clave, m] of Object.entries(data.metrics)) {
    if (!m.thresholds) continue;
    for (const [expr, res] of Object.entries(m.thresholds)) {
      umbrales.push(`| ${clave} | ${expr} | ${res.ok ? 'CUMPLE' : 'NO CUMPLE'} |`);
    }
  }
  return [
    `# Resultado k6: ${nombre}`,
    '',
    `Peticiones: **${total.count}** (${total.rate.toFixed(1)} req/s) · Tasa de error: **${(fallos * 100).toFixed(2)} %**`,
    '',
    '| Endpoint | avg (ms) | mediana (ms) | p90 (ms) | p95 (ms) | max (ms) |',
    '|----------|---------:|-------------:|---------:|---------:|---------:|',
    ...filas,
    '',
    '| Metrica | Umbral (SLO) | Estado |',
    '|---------|--------------|--------|',
    ...umbrales,
    '',
  ].join('\n');
}
