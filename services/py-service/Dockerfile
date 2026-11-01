# uv's official image = python:3.12-slim with the uv binary baked in.
FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim

WORKDIR /app

# Install pinned deps first so this layer caches until the lockfile changes.
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen

COPY app.py .

EXPOSE 8000
# --no-sync: the venv was built above; don't re-resolve at container start.
CMD ["uv", "run", "--no-sync", "uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
