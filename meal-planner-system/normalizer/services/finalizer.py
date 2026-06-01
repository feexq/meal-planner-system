from __future__ import annotations

import json
import logging

from core.llm_client import generate_json_with_fallback

log = logging.getLogger(__name__)
print("Hello from file")

# ─── System prompt ────────────────────────────────────────────────────────────

SYSTEM_PROMPT = """You are a clinical nutritionist AI. Select optimal daily meal combinations that meet calorie and macro targets.

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
- Daily calorie intake MUST NEVER exceed the target rate. Being 5-10% UNDER the target is perfectly acceptable.
- Macros (protein, carbs, fat) are a secondary goal: try to get close to the targets, but do not waste time perfecting them. Calories are the strict priority.
- Use fractional servings if needed.
- Moderate repetition: You may repeat recipes to save time, but do not use the exact same recipe more than 2-3 times across the week.
- Use only provided recipe IDs."""


# ─── Prompt builders ──────────────────────────────────────────────────────────

def _format_slot(slot: dict, top_k: int = 3) -> dict:
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


_RESPONSE_FORMAT = """\
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
      "notes": "1 short sentence summary."
    }}
  ]
}}"""

_RULES = """\
Rules for Selection:
1. COMPOSE THE PERFECT DAY: Pick the combination that together hit the daily targets.
2. MACROS ARE SECONDARY: Try to stay within ±15% of the macro targets (protein/carbs/fat), but do not waste time perfecting them. Calories are the STRICT PRIORITY.
3. USE THE SIDES POOL: You MAY add 1 item from ADDITIONAL LIGHT OPTIONS to meals if needed to hit the calorie target.
4. VALID OPTIONS ONLY: mainRecipe ID from slot's candidates. side IDs from ADDITIONAL pool only.
5. MODERATE REPETITION: You may reuse the same recipes on different days to save time, but do not use the exact same recipe more than 2-3 times across the week.
6. STRICT COMPLETENESS (CRITICAL): Return ALL 7 DAYS and ALL SLOTS. No omissions.
7. NO QUESTIONS: NEVER return a 'needClarification' JSON. YOU MUST ALWAYS GENERATE THE MEAL PLAN. If instructions conflict, use your best judgement and prioritize the dailyCalorieTarget.
- slotTotalCalories = main.estimatedCalories + (side.estimatedCalories if side else 0)
- slotTotalCalories MUST be ≤ calorieBudget
- dayTotalCalories = sum of all slotTotalCalories (MUST NEVER exceed dailyCalorieTarget. Being 5-10% UNDER the target is perfectly acceptable, but going over is strictly forbidden to avoid negative remaining calories.)
- side can be null if budget is tight or no suitable option exists
- If you use a side dish, its 'servings' MUST ALWAYS be exactly 1.0. Never scale side dishes.
- servings MUST be rounded to 1 decimal place (e.g., 0.5, 1.0, 1.2, 1.5). DO NOT use 4-decimal precision like 0.9848.
- portionNote: max 5 words (e.g. '200g cooked', '1.5 cups')
- SPEED REASONING: Solve the meal plan sequentially. Do not perform secondary math verifications. Generate the JSON immediately after your first mental draft."""


def build_finalize_prompt(request: dict) -> str:
    profile = request["userProfile"]
    days_ctx = []
    for day in request["days"]:
        print(day["dailyCalorieTarget"])
        days_ctx.append({
            "day":               day["day"],
            "dailyTargetCalories": day["dailyCalorieTarget"],
            "targetMacros": {
                "proteinG": day["estimatedDailyMacros"].get("proteinTargetG", 0),
                "carbsG":   day["estimatedDailyMacros"].get("carbsTargetG", 0),
                "fatG":     day["estimatedDailyMacros"].get("fatTargetG", 0),
            },
            "slots": [_format_slot(s) for s in day["slots"] if s.get("candidates")],
        })

    additional = [
        {"id": r["recipeId"], "name": r["name"], "type": r.get("mealType"), "calories": r["calories"],
         "macros": {"proteinG": round(r["proteinG"], 1), "carbsG": round(r["carbsG"], 1), "fatG": round(r["fatG"], 1)},
         "tags": r.get("tags", [])[:5]}
        for r in request.get("additionalRecipes", [])
    ]

    fmt = _RESPONSE_FORMAT.format(daily_cal_target=request["dailyCalorieTarget"])
    return f"""
You are an elite nutritionist AI. Compose the absolute best, most DIVERSE COMBINATION of recipes.
Calculate protein/carbs/fat for every item and sum per slot and per day.

User profile:
{json.dumps(profile, indent=2)}

MAIN Meal Candidates (select ONE best main per slot):
{json.dumps(days_ctx, indent=2)}

=== ADDITIONAL LIGHT OPTIONS (optional, if the main course doesn't fill the entire calorie budget, fill it with an additional one) ===
Low-calorie snacks, drinks, yogurts, fruits etc.
Add only when it perfectly complements the main dish.
{json.dumps(additional, indent=2)}

Return STRICT JSON ONLY — no text outside JSON, no markdown.
{fmt}

{_RULES}
"""


# ─── Public API ───────────────────────────────────────────────────────────────

def finalize_plan(request: dict) -> dict:
        prompt = build_finalize_prompt(request)

        result = generate_json_with_fallback(
            system_prompt=SYSTEM_PROMPT,
            user_prompt=prompt,
            max_tokens=20_000,
        )

        return result