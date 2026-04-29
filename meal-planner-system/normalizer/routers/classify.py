from fastapi import APIRouter

from models.schemas import ClassifyRequest, ClassifyResponse
from services.classifier import classify_batch

router = APIRouter(tags=["classify"])


@router.post("/classify", response_model=ClassifyResponse)
def classify_ingredients(req: ClassifyRequest):
    if not req.ingredients:
        return ClassifyResponse(results={}, failed=[])

    valid, failed = classify_batch(req.ingredients)

    if failed:
        retry_valid, still_failed = classify_batch(failed)
        valid.update(retry_valid)
        failed = still_failed

    return ClassifyResponse(results=valid, failed=failed)