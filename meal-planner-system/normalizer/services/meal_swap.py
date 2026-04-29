from __future__ import annotations

import json
import logging

from core.llm_client import generate_json_with_fallback

log = logging.getLogger(__name__)

# ─── System prompts ───────────────────────────────────────────────────────────

_SWAP_SIDE_SYSTEM = """You are a clinical nutritionist AI.
Your task: given a main dish, pick the SINGLE best complementary side item from the pool.

Important rules:
- You MUST try to match the "Target calories for the side dish" as closely as possible.
- Prefer items that complement the main dish (acidic to fatty, fresh to heavy, etc.) while staying close to the target calories.
- Never return null if there are valid options.
- Return ONLY valid JSON."""

_SWAP_MAIN_SYSTEM = """You are a clinical nutritionist AI.
Your task: pick the SINGLE best replacement main dish from the provided candidates
for the given meal slot.

Rules:
- Pick the candidate that best fits the slot calorie budget and user profile.
- Do NOT repeat the current recipe.
- Return ONLY valid JSON. No markdown, no explanation."""


# ─── Prompt builders ──────────────────────────────────────────────────────────

def _build_swap_side_prompt(
        slot_info: dict,
        main_recipe: dict,
        additional_pool: list[dict],
        user_profile: dict,
        calorie_budget: float,
) -> str:

    # ВИПРАВЛЕННЯ 1: calorie_budget вже є цільовим калоражем саме для цього сайду (з Java).
    # Віднімати калорії основної страви НЕ ПОТРІБНО.
    target_side_calories = calorie_budget

    # ВИПРАВЛЕННЯ 2: Java вже відфільтрувала пул (±15% від цілі).
    # Замість того, щоб брати найменші, сортуємо за тим, наскільки страва близька до нашого таргету.
    eligible = sorted(additional_pool, key=lambda r: abs(r.get("calories", 0) - target_side_calories))[:15]

    return f"""Choose the single best side item to complement this meal.

Slot: {slot_info.get("mealType", "unknown")}
Main dish already chosen:
{json.dumps(main_recipe, indent=2)}

Target calories for the side dish: {target_side_calories:.0f} kcal

User profile:
{json.dumps(user_profile, indent=2)}

Available side options (sorted by closest calorie match):
{json.dumps(eligible, indent=2)}

Return JSON:
{{
  "chosen": {{
    "recipeId": 123,
    "name": "Greek Yogurt",
    "calories": 120,
    "proteinG": 10.0,
    "carbsG": 8.0,
    "fatG": 3.0,
    "reason": "High protein, complements the main dish perfectly, and hits the calorie target"
  }}
}}

If no suitable option exists, return: {{"chosen": null}}
"""


def _build_swap_main_prompt(
        slot_info: dict,
        current_recipe_id: int,
        candidates: list[dict],
        user_profile: dict,
        calorie_budget: float,
) -> str:
    filtered = [c for c in candidates if c.get("recipeId") != current_recipe_id]
    return f"""Choose the single best replacement main dish for this slot.

Slot: {slot_info.get("mealType", "unknown")} — calorie budget: {calorie_budget:.0f} kcal
Current recipe ID (do NOT pick this): {current_recipe_id}

User profile:
{json.dumps(user_profile, indent=2)}

Replacement candidates:
{json.dumps(filtered, indent=2)}

Return JSON:
{{
  "chosen": {{
    "recipeId": 456,
    "name": "Chicken Salad",
    "scaledCalories": 480.0,
    "recommendedServings": 1.0,
    "proteinG": 35.0,
    "carbsG": 12.0,
    "fatG": 18.0,
    "reason": "Fits budget, high protein, matches user goals"
  }}
}}
"""


# ─── Public API ───────────────────────────────────────────────────────────────

def swap_side_via_llm(
        slot_info: dict,
        main_recipe: dict,
        additional_pool: list[dict],
        user_profile: dict,
        calorie_budget: float,
) -> dict | None:
    prompt = _build_swap_side_prompt(
        slot_info, main_recipe, additional_pool, user_profile, calorie_budget
    )
    try:
        result = generate_json_with_fallback(
            system_prompt=_SWAP_SIDE_SYSTEM,
            user_prompt=prompt,
            max_tokens=1_000,
            temperature=0.1,
        )
        return result.get("chosen")
    except Exception as exc:
        log.error("swap_side_via_llm failed: %s", exc)
        return None


def swap_main_via_llm(
        slot_info: dict,
        current_recipe_id: int,
        candidates: list[dict],
        user_profile: dict,
        calorie_budget: float,
) -> dict | None:
    prompt = _build_swap_main_prompt(
        slot_info, current_recipe_id, candidates, user_profile, calorie_budget
    )
    try:
        result = generate_json_with_fallback(
            system_prompt=_SWAP_MAIN_SYSTEM,
            user_prompt=prompt,
            max_tokens=1_000,
            temperature=0.3,
        )
        return result.get("chosen")
    except Exception as exc:
        log.error("swap_main_via_llm failed: %s", exc)
        return None