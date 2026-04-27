from fastapi import APIRouter, HTTPException

from core.cache import get_cached_nutrition
from models.schemas import FoodItem, ParseFoodRequest, ParseFoodResponse
from services.food_parser import parse_food_text

router = APIRouter(tags=["food"])


@router.post("/parse-food", response_model=ParseFoodResponse)
def parse_food(request: ParseFoodRequest):
    if not request.text.strip():
        raise HTTPException(status_code=400, detail="Food text cannot be empty")

    data = parse_food_text(request.text)
    return ParseFoodResponse(
        items=[FoodItem(**i) for i in data["items"]],
        total_calories=data["total_calories"],
        total_protein_g=data["total_protein_g"],
        total_carbs_g=data["total_carbs_g"],
        total_fat_g=data["total_fat_g"],
        parse_note=data.get("parse_note"),
    )


@router.get("/food-cache/{food_name}")
def get_food_cache(food_name: str):
    cached = get_cached_nutrition(food_name)
    if cached:
        return {"food": food_name, "cached": True, "nutrition": cached}
    return {"food": food_name, "cached": False}