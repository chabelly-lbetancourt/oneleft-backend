# The ranker service (HU-012). The model is trained while the image is built, from a seeded synthetic history.
FROM python:3.13-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY ranker ranker
RUN python -m ranker.train > /dev/null
RUN useradd --system --no-create-home ranker
USER ranker
EXPOSE 8086
HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=5 \
  CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8086/health').status == 200 else 1)"
CMD ["uvicorn", "ranker.main:app", "--host", "0.0.0.0", "--port", "8086"]
