# OneLeft · ranker

Pequeño servicio en Python (FastAPI + scikit-learn) que puntúa **la probabilidad de que cada persona se apunte a un
plan** (HU-012). `notifications` lo usa para avisar primero a quien más probabilidad tiene.

| Endpoint | Para qué |
|---|---|
| `POST /v1/score` | Recibe hasta 5000 candidatos con sus variables y devuelve su probabilidad, en el mismo orden |
| `GET /health` | Estado y versión del modelo |

Variables (`ranker/features.py`): distancia y distancia relativa a su radio, minutos hasta el inicio, hora y día de la
semana, si la actividad está en su lista, si viene de una alerta guardada, su tasa de aceptación reciente (suavizada:
`(aceptados + 1) / (avisos + 4)`), cuántos avisos ha recibido y cuántos hoy, y las plazas libres.

## Modelo y datos

No hay historial real antes del lanzamiento: el modelo (gradient boosting) se **entrena al construir la imagen** con
un historial sintético reproducible (`ranker/synthetic.py`, semilla fija). La forma en que se acepta está escrita en
ese fichero. `notifications` guarda los resultados reales (`sent_notice.accepted_at`) para reentrenar con ellos.

## Cómo se ejecuta

```bash
python3 -m venv .venv && . .venv/bin/activate && pip install -r requirements-dev.txt
```

```bash
python -m pytest -q
```

```bash
python -m ranker.train && uvicorn ranker.main:app --port 8087
```

## Simulador: modelo contra regla de distancia

```bash
python -m ranker.evaluate --plans 2000 --batch 5 --interval 3
```

Cada plan simulado tiene un grupo de candidatos y unas plazas. Las dos estrategias avisan por tandas hasta completar el
plan y solo se diferencian en a quién avisan primero (el más cercano, o el más probable según el modelo). El mismo
sorteo decide si cada candidato acepta, así que se comparan con las mismas personas. Mide planes completados, minutos
hasta completarlos, tasa de aceptación y avisos por plan.
