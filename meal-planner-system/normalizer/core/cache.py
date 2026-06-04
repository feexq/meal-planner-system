import json
import os

import redis

REDIS_HOST = os.environ.get("REDIS_HOST", "localhost")
REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))
REDIS_DB   = int(os.environ.get("REDIS_DB", 1))
REDIS_TTL  = 60 * 60 * 24 * 30  # 30 days

from redis.backoff import ExponentialBackoff
from redis.retry import Retry
from redis.exceptions import ConnectionError, TimeoutError

redis_client = redis.Redis(
    host=REDIS_HOST,
    port=REDIS_PORT,
    db=REDIS_DB,
    decode_responses=True,
    health_check_interval=30,
    retry_on_timeout=True,
    retry_on_error=[ConnectionError, TimeoutError, ConnectionResetError],
    retry=Retry(ExponentialBackoff(cap=10, base=1), 3)
)


def _cache_key(food_name: str) -> str:
    import re
    normalized = re.sub(r"[^a-z0-9\s]", "", food_name.lower().strip())
    normalized = re.sub(r"\s+", "_", normalized)
    return f"food_nutrition:{normalized}"


def get_cached_nutrition(food_name: str) -> dict | None:
    raw = redis_client.get(_cache_key(food_name))
    return json.loads(raw) if raw else None


def cache_nutrition(food_name: str, nutrition: dict) -> None:
    redis_client.setex(_cache_key(food_name), REDIS_TTL, json.dumps(nutrition, ensure_ascii=False))