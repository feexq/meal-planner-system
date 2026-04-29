from __future__ import annotations

from core.cache import cache_nutrition, get_cached_nutrition
from core.llm_client import generate_json_with_fallback

SYSTEM_PROMPT = """You are a nutrition database expert.
Parse the user's food intake description and return accurate nutritional estimates.

Rules:
- Estimate based on standard serving sizes and common preparation methods.
- If quantity is not specified, assume 1 standard serving.
- confidence: "high" (well-known food), "medium" (common dish), "low" (vague description).
- Always return valid JSON. No markdown. No text outside JSON."""

USER_TEMPLATE = """Parse this food intake and estimate nutrition per item.

Input: "{text}"

Return JSON only:
{{
  "items": [
    {{
      "name": "normalized english food name",
      "original": "as user wrote it",
      "quantity_description": "3 slices / 1 glass / 200g",
      "calories": 720,
      "protein_g": 30,
      "carbs_g": 84,
      "fat_g": 27,
      "confidence": "high|medium|low"
    }}
  ],
  "total_calories": 830,
  "total_protein_g": 32,
  "total_carbs_g": 110,
  "total_fat_g": 27,
  "parse_note": "optional note if something was unclear"
}}"""


def _safe_float(val) -> float:
    return float(val) if val is not None else 0.0


def parse_food_text(text: str) -> dict:
    """
    Returns parsed + cached nutrition dict matching ParseFoodResponse shape.
    """
    prompt = USER_TEMPLATE.format(text=text.strip())
    parsed = generate_json_with_fallback(
        system_prompt=SYSTEM_PROMPT,
        user_prompt=prompt,
        max_tokens=8_000,
        temperature=0.1,
    )

    items       = []
    total_cal   = 0.0
    total_prot  = 0.0
    total_carbs = 0.0
    total_fat   = 0.0

    for item_data in parsed.get("items", []):
        name       = item_data.get("name", "Unknown food")
        calories   = _safe_float(item_data.get("calories"))
        protein_g  = _safe_float(item_data.get("protein_g"))
        carbs_g    = _safe_float(item_data.get("carbs_g"))
        fat_g      = _safe_float(item_data.get("fat_g"))

        from_cache = bool(get_cached_nutrition(name))
        if not from_cache:
            cache_nutrition(name, {
                "calories":  calories,
                "protein_g": protein_g,
                "carbs_g":   carbs_g,
                "fat_g":     fat_g,
                "quantity":  item_data.get("quantity_description", "1 serving"),
            })

        items.append({
            "name":                 name,
            "original":             item_data.get("original", name),
            "quantity_description": item_data.get("quantity_description", "1 serving"),
            "calories":             calories,
            "protein_g":            protein_g,
            "carbs_g":              carbs_g,
            "fat_g":                fat_g,
            "confidence":           item_data.get("confidence", "medium"),
            "from_cache":           from_cache,
        })
        total_cal   += calories
        total_prot  += protein_g
        total_carbs += carbs_g
        total_fat   += fat_g

    return {
        "items":          items,
        "total_calories": round(total_cal, 1),
        "total_protein_g":round(total_prot, 1),
        "total_carbs_g":  round(total_carbs, 1),
        "total_fat_g":    round(total_fat, 1),
        "parse_note":     parsed.get("parse_note"),
    }