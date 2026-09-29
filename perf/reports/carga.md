# Resultado k6: carga

Peticiones: **42286** (352.2 req/s) · Tasa de error: **0.00 %**

| Endpoint | avg (ms) | mediana (ms) | p90 (ms) | p95 (ms) | max (ms) |
|----------|---------:|-------------:|---------:|---------:|---------:|
| POST /seguimiento/revisar | 16.9 | 16.3 | 27.2 | 33.8 | 37.2 |
| POST /incidencias | 1.3 | 1.0 | 2.5 | 3.2 | 14.3 |
| GET /incidencias/:id | 0.8 | 0.6 | 1.4 | 2.0 | 10.8 |
| POST /incidencias/qr | 1.3 | 1.1 | 2.4 | 3.1 | 120.8 |

| Metrica | Umbral (SLO) | Estado |
|---------|--------------|--------|
| checks | rate>0.99 | CUMPLE |
| http_req_duration{name:POST /seguimiento/revisar} | p(95)<=1000 | CUMPLE |
| http_req_duration{name:POST /incidencias} | p(95)<=500 | CUMPLE |
| http_req_failed | rate<0.01 | CUMPLE |
| http_req_duration{name:GET /incidencias/:id} | p(95)<=500 | CUMPLE |
| http_req_duration{name:POST /incidencias/qr} | p(95)<=500 | CUMPLE |
