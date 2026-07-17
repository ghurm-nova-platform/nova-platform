FROM python:3.12-slim AS builder

WORKDIR /build
RUN pip install --no-cache-dir poetry==2.4.1 \
    && poetry config virtualenvs.create false

COPY pyproject.toml README.md ./
COPY src ./src
RUN poetry install --only main --no-interaction --no-ansi

FROM python:3.12-slim AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    APP_NAME=nova-agent-runtime \
    HOST=0.0.0.0 \
    PORT=8090

WORKDIR /app
RUN useradd --create-home --uid 10001 appuser

COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin
COPY pyproject.toml README.md ./
COPY src ./src

USER appuser
EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8090/health')"

CMD ["uvicorn", "agent_runtime.main:create_app", "--factory", "--host", "0.0.0.0", "--port", "8090"]
