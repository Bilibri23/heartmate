# RoomBay AI Orchestrator (LangGraph sidecar)
FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PORT=8131

WORKDIR /app

# Install deps first for better layer caching.
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# Non-root runtime.
RUN useradd --create-home --uid 1001 orchestrator
USER orchestrator

EXPOSE 8131

# Honor $PORT (Railway/Heroku style); default 8131.
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8131}"]
