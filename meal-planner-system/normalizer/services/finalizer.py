"""
Meal-plan finalization service.
Builds Gemini (verbose) and Groq/Llama (compressed) prompts,
calls generate_json_with_fallback, and returns structured days.
"""
from __future__ import annotations

import json
import logging

from core.llm_client import generate_json_with_fallback

log = logging.getLogger(__name__)

# ─── System prompts ───────────────────────────────────────────────────────────

_GEMINI_SYSTEM = """You are a clinical nutritionist AI. Select optimal daily meal combinations that meet calorie and macro targets.

Your job:
1. Select the BEST main recipe for each meal slot.
2. Optionally add 1 side item per slot from the "additional" pool if it fits the calorie budget.
3. Specify portion sizes in grams or servings to stay within calorie targets.
4. Write daily notes with portion adjustments.
5. Calculate accurate macros for every item and daily totals.

Rules:
- Pick 1 main per slot from candidates.
- Optional: add 1 side from additional pool if within calorie budget.
- Do not exceed slot calorieBudget.
- Daily calories must be within ±15% of target.
- Daily macros must stay within min/max bounds.
- Use fractional servings if needed.
- Minimize repetition across days.
- Use only provided recipe IDs."""

_LLAMA_SYSTEM = """You are an elite nutritionist AI composing optimal daily meal combinations.
Key: cal=calories, p=proteinG, c=carbsG, f=fatG, srv=servings, n=name, tgt_m=[minG, maxG]

Return STRICT JSON ONLY. Ensure perfect math (slotTotalCalories = main.cal + side.cal).
Format EXACTLY like this:
{
  "days": [
    {
      "day": 1,
      "dailyCalorieTarget": 2000,
      "slots": [
        {
          "mealType": "breakfast",
          "calorieBudget": 600,
          "main": {
            "recipeId": 123,
            "name": "Oatmeal",
            "servings": 1.0,
            "estimatedCalories": 380,
            "portionNote": "1 cup cooked",
            "proteinG": 12.0,
            "carbsG": 64.0,
            "fatG": 7.0
          },
          "side": null
        }
      ],
      "dayTotalCalories": 1980,
      "dailyProteinG": 105.0,
      "dailyCarbsG": 240.0,
      "dailyFatG": 58.0,
      "notes": "Short explanation for this day."
    }
  ]
}

Rules:
1. SUM of macros must fit tgt_m boundaries per day.
2. USE SIDES POOL: Add 1 side to Lunch/Dinner if budget allows. MUST use >=3 per day across slots.
3. VALID IDs ONLY from provided input.
4. MUST RETURN ALL 7 DAYS AND ALL SLOTS. No omissions. From day 1 to day 7
5. CRITICAL: NO MATH IN JSON. Output ONLY final calculated floats."""


# ─── Prompt builders ──────────────────────────────────────────────────────────

def _gemini_slot(slot: dict, top_k: int = 3) -> dict:
    candidates = sorted(slot["candidates"], key=lambda c: c["score"], reverse=True)[:top_k]
    return {
        "mealType":     slot["mealType"],
        "calorieBudget": slot["slotCalorieBudget"],
        "options": [
            {
                "id":            c["recipeId"],
                "name":          c["recipeName"],
                "score":         c["score"],
                "scaledCalories": round(c["scaledCalories"], 1),
                "macros":        {"proteinG": round(c["proteinG"], 1), "carbsG": round(c["carbsG"], 1), "fatG": round(c["fatG"], 1)},
                "servings":      c["recommendedServings"],
            }
            for c in candidates
        ],
    }


def _llama_slot(slot: dict, top_k: int = 3) -> dict:
    candidates = sorted(slot["candidates"], key=lambda c: c["score"], reverse=True)[:top_k]
    return {
        "type":   slot["mealType"],
        "budget": slot["slotCalorieBudget"],
        "opts": [
            {
                "id": c["recipeId"], "n": c["recipeName"], "cal": round(c["scaledCalories"], 1),
                "p": round(c["proteinG"], 1), "c": round(c["carbsG"], 1), "f": round(c["fatG"], 1),
                "srv": round(c["recommendedServings"], 2),
            }
            for c in candidates
        ],
    }


_SHARED_RESPONSE_FORMAT = """\
{{
  "days": [
    {{
      "day": 1,
      "dailyCalorieTarget": {daily_cal_target},
      "slots": [
        {{
          "mealType": "breakfast",
          "calorieBudget": 600,
          "main": {{
            "recipeId": 123,
            "name": "Oatmeal",
            "servings": 1.0,
            "estimatedCalories": 380,
            "portionNote": "1 cup cooked (240ml)",
            "proteinG": 12.0,
            "carbsG": 64.0,
            "fatG": 7.0
          }},
          "side": null
        }}
      ],
      "dayTotalCalories": 1980,
      "dailyProteinG": 105.0,
      "dailyCarbsG": 240.0,
      "dailyFatG": 58.0,
      "notes": "3-4 sentences explaining why this day fits the user's specific conditions and goals."
    }}
  ]
}}"""

_SHARED_RULES = """\
Rules for Selection:
1. COMPOSE THE PERFECT DAY: Pick the combination that together hit the daily targets.
2. MACRO BOUNDARIES: SUM of macros for ALL chosen recipes per day MUST stay within min/max bounds.
3. USE THE SIDES POOL: Strongly encouraged — add 1 item from ADDITIONAL LIGHT OPTIONS to Lunch and Dinner.
4. VALID OPTIONS ONLY: mainRecipe ID from slot's candidates. side IDs from ADDITIONAL pool only.
5. MINIMIZE REPETITION: Maximum variety across the week.
6. STRICT COMPLETENESS (CRITICAL): Return ALL 7 DAYS and ALL SLOTS. No omissions.
- slotTotalCalories = main.estimatedCalories + (side.estimatedCalories if side else 0)
- slotTotalCalories MUST be ≤ calorieBudget
- dayTotalCalories = sum of all slotTotalCalories (must be within ±15% of dailyCalorieTarget)
- side can be null if budget is tight or no suitable option exists
- servings can be fractional (0.5, 1.5) to fit the calorie budget
- portionNote must be specific: grams or household measure"""


def build_gemini_finalize_prompt(request: dict) -> str:
    profile = request["userProfile"]
    days_ctx = []
    for day in request["days"]:
        days_ctx.append({
            "day":               day["day"],
            "dailyTargetCalories": day["dailyCalorieTarget"],
            "targetMacros": {
                "protein": {"targetG": day["estimatedDailyMacros"].get("proteinTargetG", 0),
                            "minG": day["estimatedDailyMacros"].get("proteinMinG", 0),
                            "maxG": day["estimatedDailyMacros"].get("proteinMaxG", 0)},
                "carbs":   {"targetG": day["estimatedDailyMacros"].get("carbsTargetG", 0),
                            "minG": day["estimatedDailyMacros"].get("carbsAbsoluteMinG", 0)},
                "fat":     {"targetG": day["estimatedDailyMacros"].get("fatTargetG", 0),
                            "minG": day["estimatedDailyMacros"].get("fatMinG", 0),
                            "maxG": day["estimatedDailyMacros"].get("fatMaxG", 0)},
            },
            "slots": [_gemini_slot(s) for s in day["slots"] if s.get("candidates")],
        })

    additional = [
        {"id": r["recipeId"], "name": r["name"], "type": r.get("mealType"), "calories": r["calories"],
         "macros": {"proteinG": round(r["proteinG"], 1), "carbsG": round(r["carbsG"], 1), "fatG": round(r["fatG"], 1)},
         "tags": r.get("tags", [])[:5]}
        for r in request.get("additionalRecipes", [])
    ]

    fmt = _SHARED_RESPONSE_FORMAT.format(daily_cal_target=request["dailyCalorieTarget"])
    return f"""
You are an elite nutritionist AI. Compose the absolute best, most DIVERSE COMBINATION of recipes.
Calculate protein/carbs/fat for every item and sum per slot and per day.

User profile:
{json.dumps(profile, indent=2)}

MAIN Meal Candidates (select ONE best main per slot):
{json.dumps(days_ctx, indent=2)}

=== ADDITIONAL LIGHT OPTIONS (optional, ≤250 kcal each) ===
Low-calorie snacks, drinks, yogurts, fruits etc.
You MUST add AT LEAST 3 of these across the day.
Add only when it perfectly complements the main dish.
{json.dumps(additional, indent=2)}

Return STRICT JSON ONLY — no text outside JSON, no markdown.
{fmt}

{_SHARED_RULES}
"""


def build_llama_finalize_prompt(request: dict) -> str:
    profile = request["userProfile"]
    days_ctx = []
    for day in request["days"]:
        days_ctx.append({
            "d": day["day"], "tgt_cal": day["dailyCalorieTarget"],
            "tgt_m": {
                "p": [day["estimatedDailyMacros"].get("proteinMinG", 0), day["estimatedDailyMacros"].get("proteinMaxG", 0)],
                "c": [day["estimatedDailyMacros"].get("carbsAbsoluteMinG", 0), day["estimatedDailyMacros"].get("carbsTargetG", 0)],
                "f": [day["estimatedDailyMacros"].get("fatMinG", 0), day["estimatedDailyMacros"].get("fatMaxG", 0)],
            },
            "slots": [_llama_slot(s) for s in day["slots"] if s.get("candidates")],
        })

    additional = [
        {"id": r["recipeId"], "n": r["name"], "cal": r["calories"],
         "p": round(r["proteinG"], 1), "c": round(r["carbsG"], 1), "f": round(r["fatG"], 1)}
        for r in request.get("additionalRecipes", [])
    ]

    fmt = _SHARED_RESPONSE_FORMAT.format(daily_cal_target=request["dailyCalorieTarget"])
    return f"""
You are an elite nutritionist AI. Compose the BEST DIVERSE meal combination for each day.

User: {json.dumps(profile, separators=(',', ':'))}

MAIN Candidates:
{json.dumps(days_ctx, separators=(',', ':'))}

ADDITIONAL (opt, ≤250kcal each, ≥3/day):
{json.dumps(additional, separators=(',', ':'))}

Return STRICT JSON ONLY.
{fmt}

{_SHARED_RULES}
"""


# ─── Public API ───────────────────────────────────────────────────────────────

def finalize_plan(request: dict) -> dict:
    """
    request: dict matching FinalizeRequest pydantic shape (model_dump()).
    Returns parsed dict with 'days' key.
    """
    gemini_prompt = build_gemini_finalize_prompt(request)
    llama_prompt  = build_llama_finalize_prompt(request)

    return generate_json_with_fallback(
        system_prompt=_GEMINI_SYSTEM,
        user_prompt=gemini_prompt,
        fallback_system_prompt=_LLAMA_SYSTEM,
        fallback_user_prompt=llama_prompt,
        max_tokens=14_000,
    )