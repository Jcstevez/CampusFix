# Resultado k6: baseline

Peticiones: **277** (9.2 req/s) · Tasa de error: **0.00 %**

| Endpoint | avg (ms) | mediana (ms) | p90 (ms) | p95 (ms) | max (ms) |
|----------|---------:|-------------:|---------:|---------:|---------:|
| POST /incidencias | 4.3 | 4.3 | 5.4 | 5.9 | 10.5 |
| GET /incidencias/:id | 1.9 | 1.7 | 2.5 | 2.8 | 16.1 |
| POST /incidencias/qr | 6.0 | 4.2 | 5.8 | 6.5 | 132.5 |

| Metrica | Umbral (SLO) | Estado |
|---------|--------------|--------|
| http_req_duration{name:GET /incidencias/:id} | p(95)<=100 | CUMPLE |
| http_req_failed | rate==0 | CUMPLE |
| checks | rate==1 | CUMPLE |
| http_req_duration{name:POST /incidencias/qr} | p(95)<=100 | CUMPLE |
| http_req_duration{name:POST /incidencias} | p(95)<=100 | CUMPLE |
