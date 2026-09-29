# Resultado k6: estres

Peticiones: **355275** (1479.8 req/s) · Tasa de error: **0.00 %**

| Endpoint | avg (ms) | mediana (ms) | p90 (ms) | p95 (ms) | max (ms) |
|----------|---------:|-------------:|---------:|---------:|---------:|
| POST /incidencias | 1.2 | 0.9 | 1.9 | 2.7 | 184.3 |
| GET /incidencias/:id | 1.1 | 0.7 | 2.3 | 3.0 | 116.4 |
| POST /incidencias/qr | 1.8 | 1.0 | 1.9 | 2.7 | 52921.1 |

| Metrica | Umbral (SLO) | Estado |
|---------|--------------|--------|
| http_req_duration{name:POST /incidencias} | p(95)<=1000 | CUMPLE |
| http_req_duration{name:GET /incidencias/:id} | p(95)<=1000 | CUMPLE |
| http_req_duration{name:POST /incidencias/qr} | p(95)<=1000 | CUMPLE |
| http_req_failed | rate<0.05 | CUMPLE |
