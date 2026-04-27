from fastapi import APIRouter
from ingredient_parser import parse_ingredient

from models.schemas import NormalizeRequest, NormalizeResponse
from services.normalizer import normalize_ingredient_text

router = APIRouter(tags=["normalize"])


@router.post("/normalize", response_model=NormalizeResponse)
def normalize_ingredient(req: NormalizeRequest):
    parsed = parse_ingredient(req.raw_name)
    if parsed.name and len(parsed.name) > 0:
        nlp_text   = " ".join([n.text for n in parsed.name])
        normalized = normalize_ingredient_text(nlp_text)
    else:
        normalized = normalize_ingredient_text(req.raw_name)

    if not normalized:
        normalized = req.raw_name.lower().strip()

    return NormalizeResponse(raw_name=req.raw_name, normalized_name=normalized)