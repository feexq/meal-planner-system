"""
NLP Service — FastAPI entrypoint.

Routers:
  /normalize        — ingredient text normalization
  /classify         — diet/health-condition tagging (LLM)
  /parse-food       — food intake → nutrition (LLM + Redis cache)
  /finalize         — weekly meal-plan finalization (LLM)
  /swap-slot/main   — swap main dish of a slot (scored candidates)
  /swap-slot/side   — swap side dish of a slot (LLM from additional pool)
  /food-cache/{name}— Redis cache inspection
  /health           — liveness probe
"""
import logging
import sys
import os

from fastapi import FastAPI

sys.path.insert(0, os.path.dirname(__file__))

from routers import classify, finalize, food, normalize, swap

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)

app = FastAPI(
    title="Meal Planner NLP Service",
    version="2.0.0",
    description="LLM-powered nutrition microservice with 4-tier model fallback",
)

app.include_router(normalize.router)
app.include_router(classify.router)
app.include_router(food.router)
app.include_router(finalize.router)
app.include_router(swap.router)


@app.get("/health", tags=["ops"])
def health():
    return {"status": "ok", "version": "2.0.0"}