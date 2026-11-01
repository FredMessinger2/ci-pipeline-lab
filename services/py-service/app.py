"""Minimal FastAPI service exposing liveness and readiness endpoints.

Liveness = "the process is up" (always 200 once the server answers).
Readiness = "I can serve traffic" (503 until READY_DELAY_SECONDS after start,
simulating warmup such as loading a model or opening DB connections).
"""

import os
import time

from fastapi import FastAPI
from fastapi.responses import JSONResponse

READY_DELAY_SECONDS = float(os.environ.get("READY_DELAY_SECONDS", "0"))
_started = time.monotonic()

app = FastAPI()


@app.get("/health/live")
def live():
    return {"status": "alive"}


@app.get("/health/ready")
def ready():
    warmed_up = time.monotonic() - _started >= READY_DELAY_SECONDS
    if warmed_up:
        return {"status": "ready"}
    return JSONResponse({"status": "warming up"}, status_code=503)
