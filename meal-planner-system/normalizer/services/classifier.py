"""
Ingredient classification service — diet/health-condition tags.
Uses LLM fallback pipeline.
"""
from __future__ import annotations

import json
import logging

from core.llm_client import generate_json_with_fallback

log = logging.getLogger(__name__)

REQUIRED_KEYS = {
    "gastritis", "diabetes", "hypertension", "high_cholesterol", "celiac_disease",
    "lactose_intolerance", "nut_allergy", "shellfish_allergy", "fish_allergy",
    "kidney_disease", "gout", "pancreatitis", "gerd", "ibs",
    "vegetarian", "vegan", "keto", "paleo", "mediterranean", "low_calorie", "gluten_free",
}
VALID_STATUSES = {"allowed", "soft_forbidden", "hard_forbidden"}

SYSTEM_PROMPT = """You are a medical nutrition expert. Your task is to classify food ingredients according to dietary restrictions and health conditions.

For each ingredient, assign one of three statuses for EVERY diet/condition:
- "allowed"        — safe to consume
- "soft_forbidden" — should be limited or consumed with caution
- "hard_forbidden"  — must be strictly avoided

MEDICAL CONDITIONS:
- gastritis: hard_forbidden=[alcohol, coffee, caffeine, spicy peppers, chili, hot sauce, raw onion, raw garlic, vinegar, citrus juice, tomato sauce, fried foods, fatty meats]; soft_forbidden=[acidic foods, black pepper, carbonated drinks, chocolate, mint]
- diabetes: hard_forbidden=[white sugar, honey, corn syrup, maple syrup, candy, soda, white bread, white rice, pastry, cake, cookies]; soft_forbidden=[high-GI fruits, starchy vegetables, whole grains, natural fruit juice]
- hypertension: hard_forbidden=[table salt, soy sauce, fish sauce, processed meats, bacon, ham, canned soups, pickles]; soft_forbidden=[cheese, bread, shellfish, canned foods]
- high_cholesterol: hard_forbidden=[lard, butter, ghee, cream, full-fat cheese, organ meats, egg yolk, coconut oil, palm oil, fried foods]; soft_forbidden=[red meat, poultry skin, whole milk, shrimp]
- celiac_disease: hard_forbidden=[wheat, barley, rye, spelt, kamut, wheat flour, bread, pasta, beer, soy sauce]; soft_forbidden=[oats]
- lactose_intolerance: hard_forbidden=[milk, cream, ice cream, soft cheese, yogurt, butter, whey]; soft_forbidden=[hard aged cheese, ghee]
- nut_allergy: hard_forbidden=[all tree nuts, peanut, nut butter, nut oil, marzipan]; soft_forbidden=[coconut, seeds processed near nuts]
- shellfish_allergy: hard_forbidden=[shrimp, crab, lobster, clam, oyster, scallop, mussel, crawfish, squid, octopus]; soft_forbidden=[fish sauce, oyster sauce, worcestershire sauce]
- fish_allergy: hard_forbidden=[all fish species, fish sauce, anchovy, worcestershire sauce, caesar dressing]; soft_forbidden=[anything processed in fish facilities]
- kidney_disease: hard_forbidden=[banana, orange, potato, tomato, dairy, nuts, seeds, cola, organ meats]; soft_forbidden=[whole grains, legumes, red meat]
- gout: hard_forbidden=[organ meats, anchovies, sardines, herring, mackerel, shellfish, beer, alcohol, red meat]; soft_forbidden=[chicken, pork, beef, fish, spinach, mushroom, asparagus, cauliflower, legumes]
- pancreatitis: hard_forbidden=[alcohol, fried foods, fatty meats, cream, full-fat cheese, butter, lard, coconut oil]; soft_forbidden=[red meat, egg yolk, avocado, nuts]
- gerd: hard_forbidden=[coffee, alcohol, chocolate, mint, peppermint, tomato sauce, citrus juice, spicy foods, fried foods, onion, garlic]; soft_forbidden=[carbonated drinks, high-fat foods, vinegar, citrus fruits]
- ibs: hard_forbidden=[garlic, onion, wheat, rye, milk, soft cheese, apple, pear, mango, stone fruits, beans, lentils, cauliflower, mushroom]; soft_forbidden=[caffeine, alcohol, spicy foods, fatty foods]

DIETS:
- vegetarian: hard_forbidden=[beef, pork, chicken, turkey, lamb, veal, duck, fish, shellfish, seafood, gelatin, lard, meat broth]; soft_forbidden=[rennet cheese]
- vegan: hard_forbidden=[all meat, fish, seafood, eggs, milk, cream, cheese, butter, honey, gelatin, lard, whey, casein]; soft_forbidden=[some wines, some sugar]
- keto: hard_forbidden=[sugar, bread, pasta, rice, potato, corn, most fruits, beans, legumes, oats, most grains]; soft_forbidden=[some dairy, high-carb vegetables, alcohol]
- paleo: hard_forbidden=[grains, legumes, dairy, refined sugar, salt, processed foods, vegetable oils]; soft_forbidden=[white potato, alcohol, coffee]
- mediterranean: soft_forbidden=[red meat, butter, cream, processed foods, refined sugar, refined grains]
- low_calorie: hard_forbidden=[sugar, candy, chips, fried foods, full-fat cheese, cream, butter]; soft_forbidden=[bread, pasta, rice, starchy vegetables, dried fruits, nuts]
- gluten_free: hard_forbidden=[wheat, barley, rye, spelt, farro, wheat flour, bread, pasta, beer, soy sauce, seitan]; soft_forbidden=[oats, malt vinegar]

IMPORTANT RULES:
1. If ingredient is ambiguous → use "soft_forbidden" where doubt exists
2. Classify the ingredient AS-IS
3. For compound ingredients — classify based on the most restrictive component
4. Be consistent across the batch

Respond ONLY with a valid JSON object. No markdown, no explanation, no backticks.
Format:
{
  "ingredient_name": {
    "gastritis": "allowed",
    ...all 21 keys...
  }
}"""


def _build_user_prompt(batch: list[str]) -> str:
    return f"""Classify these ingredients. Return JSON only.

Ingredients to classify:
{json.dumps(batch, ensure_ascii=False)}

Required keys for each ingredient (all 21):
gastritis, diabetes, hypertension, high_cholesterol, celiac_disease,
lactose_intolerance, nut_allergy, shellfish_allergy, fish_allergy,
kidney_disease, gout, pancreatitis, gerd, ibs,
vegetarian, vegan, keto, paleo, mediterranean, low_calorie, gluten_free

Allowed values: "allowed", "soft_forbidden", "hard_forbidden"
"""


def classify_batch(batch: list[str]) -> tuple[dict, list[str]]:
    result = generate_json_with_fallback(
        system_prompt=SYSTEM_PROMPT,
        user_prompt=_build_user_prompt(batch),
        max_tokens=2500,
    )

    valid: dict   = {}
    failed: list  = []

    for name in batch:
        if name not in result:
            failed.append(name)
            continue
        tags    = result[name]
        missing = REQUIRED_KEYS - set(tags.keys())
        invalid = [k for k, v in tags.items() if v not in VALID_STATUSES]
        if missing or invalid:
            failed.append(name)
        else:
            valid[name] = tags

    return valid, failed