import os
import json
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Any
from google import genai
from google.genai import types
from openai import OpenAI
import re
import inflect
import urllib.parse
from ingredient_parser import parse_ingredient
import redis
import hashlib

app = FastAPI()

# ── Gemini config ──
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "")
GROQ_API_KEY = os.environ.get("GROQ_API_KEY", "")
groq_client = OpenAI(
    api_key=GROQ_API_KEY if GROQ_API_KEY else "dummy",
    base_url="https://api.groq.com/openai/v1"
)

MODEL = "gemma-4-31b-it"
gemini_client = genai.Client(api_key=GEMINI_API_KEY)

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

REQUIRED_KEYS = {
    "gastritis", "diabetes", "hypertension", "high_cholesterol", "celiac_disease",
    "lactose_intolerance", "nut_allergy", "shellfish_allergy", "fish_allergy",
    "kidney_disease", "gout", "pancreatitis", "gerd", "ibs",
    "vegetarian", "vegan", "keto", "paleo", "mediterranean", "low_calorie", "gluten_free"
}
VALID_STATUSES = {"allowed", "soft_forbidden", "hard_forbidden"}

# ─────────────────────────────────────────────
# Regex Cleaners
# ─────────────────────────────────────────────
_QUANTITY_RE = re.compile(r'\b\d+[\d/\.\s]*%?(?!-[a-zA-Z])|%\d+|\d+%25|%25|\b\d+[\.\d]*\s*(?!-[a-zA-Z])')
_SIZE_RE     = re.compile(r'\b\d+[\-\s]?(?:inch|in\b|cm\b|mm\b|quart\b|qt\b)')
_PAREN_RE    = re.compile(r'\(.*?\)')
_SPACE_RE    = re.compile(r'\s+')
_TRADEMARK_RE = re.compile(r'&reg;|&amp;|®|™', re.IGNORECASE)
_BRAND_NAME_RE = re.compile(
    r'\b(pillsbury\s+best|pillsbury|crisco|bisquick|jell-?o|kraft|heinz|'
    r'campbells?|hidden\s+valley|rosarita|ro\s*tel|market\s+pantry|'
    r'simply\s+potatoes?|swanson|velveeta|philadelphia|hellmann\'?s|'
    r'lipton|progresso|betty\s+crocker|land\s+o\s+lakes)\b',
    re.IGNORECASE
)
_TAIL_RE = re.compile(
    r'\s+(?:packed\s+in|in|with)\s+'
    r'(?:water|brine|puree|oil|vegetable\s+oil|tomato\s+puree|tomato\s+juice)$',
    re.IGNORECASE
)

KEEP_TAIL_PHRASES = {
    "bean with bacon soup", "cheese with green chili pepper",
    "cheese with jalapeno pepper", "cream cheese with chive and onion",
    "english muffin with raisin", "mutton bone with marrow",
    "pasta sauce with mushroom", "pork and bean in tomato sauce",
    "refried bean with green chili", "refried bean with jalapeno",
    "tomato with basil oregano and garlic", "tomato with green chili",
    "tomato with jalapeno", "ranch style black-eyed pea with jalapeno",
    "chipotle chile in adobo", "bean in tomato sauce",
    "simply potato potato with onion", "silver cachou",
}

STRIP_WORDS = {
    "chopped", "minced", "diced", "sliced", "grated", "shredded",
    "crushed", "mashed", "pureed", "blended", "ground", "beaten",
    "softened", "melted", "cooked", "baked", "fried", "steamed",
    "frozen", "thawed", "dried", "fresh", "raw", "canned", "jarred",
    "roasted", "toasted", "boiled", "grilled", "smashed", "flaked",
    "pickled", "marinated", "soaked", "drained", "rinsed", "peeled",
    "seeded", "pitted", "trimmed", "halved", "quartered", "cubed",
    "large", "small", "medium", "big", "whole", "half", "extra", "halves",
    "jumbo", "mini", "baby", "young", "mature", "ripe", "thick", "thin",
    "cup", "cups", "tbsp", "tsp", "tablespoon", "tablespoons",
    "teaspoon", "teaspoons", "oz", "ounce", "ounces",
    "lb", "lbs", "pound", "pounds", "gram", "grams", "kg",
    "ml", "liter", "liters", "clove", "cloves", "piece", "pieces",
    "slice", "slices", "bunch", "bunches", "head", "heads",
    "stalk", "stalks", "sprig", "sprigs", "can", "cans",
    "jar", "jars", "package", "packages", "pkg", "bag", "bags",
    "quart", "quarts", "pint", "pints",
    "unsalted", "salted", "sweetened", "unsweetened", "organic",
    "free-range", "homemade", "store-bought", "instant", "quick",
    "old-fashioned", "regular", "plain", "natural", "pure",
    "optional", "taste", "needed", "fine", "coarse",
    "light", "lite", "aged", "barbecued", "blanched", "boiling",
    "bottled", "brewed", "broken", "candied", "center-cut", "chunky",
    "cold", "condensed", "cracked", "creamy", "crisp", "crispy",
    "crunchy", "crusty", "dark", "day-old", "dehydrated", "deli",
    "diet", "distilled", "double", "dry", "edible", "firm", "flavored",
    "granulated", "hard", "hard-boiled", "hard-cooked", "heavy",
    "imitation", "mild", "miniature", "mixed", "part-skim",
    "popped", "powdered", "prepared", "processed", "ready-made",
    "real", "refrigerated", "seasoned", "seedless",
    "semi-sweet", "semisweet", "sharp", "smoked", "smooth", "soft",
    "solid", "sour", "spicy", "stewed", "stewing", "strong", "stuffed",
    "tart", "unbaked", "unbleached", "uncooked", "unflavored", "unpopped",
    "unsifted", "unsmoked", "unsulphured", "warm", "whipped",
    "low-sodium", "reduced-sodium", "no-salt", "no-salt-added", "sodium-free",
    "reduced-fat", "low-fat", "fat-free", "nonfat", "non-fat",
    "sugar-free", "gluten-free", "dairy-free", "low-carb", "low-calorie",
    "reduced-calorie", "non-dairy", "vegan", "vegetarian",
    "lean", "boneless", "skinless", "skin-on", "bone-in",
    "breast", "breasts", "thigh", "thighs", "wing", "wings",
    "leg", "legs", "fillet", "fillets", "cutlet", "cutlets",
    "ready-to-serve", "ready-to-bake", "quick-cooking"
}

REPAIR_MAP = {
    'asparagu': 'asparagus',
    'couscou':  'couscous',
    'hummu':    'hummus',
    'cachou':   'cachous',
    'watercres':'watercress',
}

NO_SINGULARIZE = {
    "asparagus", "couscous", "hummus", "quinoa", "shoyu",
    "molasses", "watercress", "tahini", "tzatziki", "harissa",
    "kombucha", "tempeh", "miso", "tofu", "edamame",
    "oats", "grits", "lentils", "greens", "herbs", "sprouts",
    "breadcrumbs", "bitters", "anchovies", "series", "species",
    "fish", "deer", "sheep", "moose",
    "cachou", "orzo", "panko", "pesto", "risotto", "polenta",
    "mascarpone", "prosciutto", "pancetta", "chorizo", "andouille",
    "guacamole", "salsa", "aioli", "tapenade", "chimichurri",
}

p = inflect.engine()


# ─────────────────────────────────────────────
# Helper functions (повністю з твого скрипту)
# ─────────────────────────────────────────────

def strip_brands(text):
    text = _TRADEMARK_RE.sub('', text)
    text = _BRAND_NAME_RE.sub('', text)
    return text.strip(" -,.'\"")


def strip_tail(text):
    if text in KEEP_TAIL_PHRASES:
        return text
    return _TAIL_RE.sub('', text).strip()


def safe_singularize(word):
    if word in NO_SINGULARIZE:
        return word
    result = p.singular_noun(word)
    return result if result else word


def clean_descriptor_words(words):
    cleaned = []
    for word in words:
        if word in STRIP_WORDS:
            continue
        if len(word) == 1 and word not in ('a', 'i'):
            continue
        if re.match(r'^[\d\%\-\+]+$', word):
            continue
        cleaned.append(word)
    return cleaned


def deep_clean_ingredient_text(text):
    text = urllib.parse.unquote(text)
    text = text.lower().strip()

    if text in REPAIR_MAP:
        return REPAIR_MAP[text]

    words_check = text.split()
    if words_check and words_check[-1] in REPAIR_MAP:
        words_check[-1] = REPAIR_MAP[words_check[-1]]
        text = ' '.join(words_check)

    text = strip_brands(text)
    if not text:
        return ''

    parts = [pt.strip() for pt in text.split(',')]
    main_part   = parts[0]
    descriptors = ' '.join(parts[1:]) if len(parts) > 1 else ''

    for part_name in ('main_part', 'descriptors'):
        val = locals()[part_name]
        val = _PAREN_RE.sub('', val)
        val = _SIZE_RE.sub('', val)
        val = _QUANTITY_RE.sub('', val)
        val = _SPACE_RE.sub(' ', val).strip()
        if part_name == 'main_part':
            main_part = val
        else:
            descriptors = val

    final_text = f"{main_part} {descriptors}".strip()
    final_text = strip_tail(final_text)

    words = final_text.split()
    words = clean_descriptor_words(words)

    normalized_words = []
    for word in words:
        if word in ("of", "zest", "juice", "peel"):
            normalized_words.append(word)
            continue
        normalized_words.append(safe_singularize(word))

    return ' '.join(normalized_words)


# ─────────────────────────────────────────────
# API
# ─────────────────────────────────────────────

class NormalizeRequest(BaseModel):
    raw_name: str

class NormalizeResponse(BaseModel):
    raw_name: str
    normalized_name: str

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/normalize", response_model=NormalizeResponse)
def normalize_ingredient(req: NormalizeRequest):
    parsed = parse_ingredient(req.raw_name)
    
    if parsed.name and len(parsed.name) > 0:
        nlp_text = " ".join([n.text for n in parsed.name])
        normalized = deep_clean_ingredient_text(nlp_text)
    else:
        normalized = deep_clean_ingredient_text(req.raw_name)

    if not normalized:
        normalized = req.raw_name.lower().strip()

    return NormalizeResponse(
        raw_name=req.raw_name,
        normalized_name=normalized
    )

class ClassifyRequest(BaseModel):
    ingredients: list[str]  # список normalized_name

class ClassifyResponse(BaseModel):
    results: dict  # normalized_name → {condition: status}
    failed: list[str]  # які не вдалось класифікувати


def build_user_prompt(batch: list[str]) -> str:
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
        user_prompt=build_user_prompt(batch),
        max_tokens=2500
    )

    valid = {}
    failed = []

    for name in batch:
        if name not in result:
            failed.append(name)
            continue

        tags = result[name]

        missing = REQUIRED_KEYS - set(tags.keys())
        invalid = [k for k, v in tags.items() if v not in VALID_STATUSES]

        if missing or invalid:
            failed.append(name)
        else:
            valid[name] = tags

    return valid, failed


@app.post("/classify", response_model=ClassifyResponse)
def classify_ingredients(req: ClassifyRequest):
    if not req.ingredients:
        return ClassifyResponse(results={}, failed=[])

    valid, failed = classify_batch(req.ingredients)

    if failed:
        retry_valid, still_failed = classify_batch(failed)
        valid.update(retry_valid)
        failed = still_failed

    return ClassifyResponse(results=valid, failed=failed)

# ─────────────────────────────────────────────
# Finalize Prompts & Builders
# ─────────────────────────────────────────────

GEMINI_FINALIZE_SYSTEM_PROMPT = """You are a clinical nutritionist AI. Select optimal daily meal combinations that meet calorie and macro targets.

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

LLAMA_FINALIZE_SYSTEM_PROMPT = """You are an elite nutritionist AI composing optimal daily meal combinations.
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
2. USE SIDES POOL: Add 1 side to Lunch/Dinner if budget allows, to hit targets. MUST use >=3 per day across slots.
3. VALID IDs ONLY from provided input.
4. MUST RETURN ALL 7 DAYS AND ALL SLOTS. No omissions. From day 1 to day 7
5. CRITICAL: NO MATH IN JSON. You must calculate the final numbers internally. Do NOT output equations or expressions like `803.7 * 0.8`. Output ONLY the final calculated float (e.g., `642.9`)."""


# --- Gemini Prompt Builders (Verbose) ---
class NutritionTargets(BaseModel):
    proteinTargetG: int
    carbsTargetG: int
    fatTargetG: int
    proteinCoveragePercent: int | None = None
    carbsStatus: str | None = None
    fatStatus: str | None = None

class CandidateItem(BaseModel):
    recipeId: int
    recipeName: str
    score: int
    caloriesPerServing: float
    recommendedServings: float
    scaledCalories: float
    proteinG: float = 0.0
    carbsG: float = 0.0
    fatG: float = 0.0
    ingredients: list[str] = []

class SlotItem(BaseModel):
    mealType: str
    slotCalorieBudget: int
    candidates: list[CandidateItem]

class DayItem(BaseModel):
    day: int
    dailyCalorieTarget: int
    estimatedDailyMacros: dict[str, Any] = {}
    slots: list[SlotItem]

class UserProfileRequest(BaseModel):
    gender: str
    age: int
    weightKg: float
    heightCm: int
    activityLevel: str
    goal: str
    dietType: str
    healthConditions: list[str] = []
    allergies: list[str] = []
    dislikedIngredients: list[str] = []
    mealsPerDay: int

class AdditionalRecipeItem(BaseModel):
    recipeId: int
    name: str
    mealType: str | None = None
    calories: float
    proteinG: float
    carbsG: float
    fatG: float
    ingredients: list[str] = []
    tags: list[str] = []

class FinalizeRequest(BaseModel):
    userId: str
    dailyCalorieTarget: int
    weeklyCalorieTarget: int
    days: list[DayItem]
    userProfile: UserProfileRequest
    additionalRecipes: list[AdditionalRecipeItem] = []

def _build_gemini_slot_candidates(slot: SlotItem, top_k: int = 3) -> dict:
    sorted_candidates = sorted(slot.candidates, key=lambda c: c.score, reverse=True)[:top_k]
    return {
        "mealType": slot.mealType,
        "calorieBudget": slot.slotCalorieBudget,
        "options": [
            {
                "id": c.recipeId,
                "name": c.recipeName,
                "score": c.score,
                "scaledCalories": round(c.scaledCalories, 1),
                "macros": {
                    "proteinG": round(c.proteinG, 1),
                    "carbsG": round(c.carbsG, 1),
                    "fatG": round(c.fatG, 1)
                },
                "servings": c.recommendedServings,
            }
            for c in sorted_candidates
        ]
    }

def _build_gemini_finalize_prompt(request: FinalizeRequest) -> str:
    profile = request.userProfile
    days_context = []
    for day in request.days:
        day_data = {
            "day": day.day,
            "dailyTargetCalories": day.dailyCalorieTarget,
            "targetMacros": {
                "protein": {"targetG": day.estimatedDailyMacros.get("proteinTargetG", 0), "minG": day.estimatedDailyMacros.get("proteinMinG", 0), "maxG": day.estimatedDailyMacros.get("proteinMaxG", 0)},
                "carbs": {"targetG": day.estimatedDailyMacros.get("carbsTargetG", 0), "minG": day.estimatedDailyMacros.get("carbsAbsoluteMinG", 0)},
                "fat": {"targetG": day.estimatedDailyMacros.get("fatTargetG", 0), "minG": day.estimatedDailyMacros.get("fatMinG", 0), "maxG": day.estimatedDailyMacros.get("fatMaxG", 0)}
            },
            "slots": [_build_gemini_slot_candidates(slot) for slot in day.slots if slot.candidates]
        }
        days_context.append(day_data)

    additional_context = [
        {
            "id": r.recipeId, "name": r.name, "type": r.mealType, "calories": r.calories,
            "macros": {"proteinG": round(r.proteinG, 1), "carbsG": round(r.carbsG, 1), "fatG": round(r.fatG, 1)},
            "tags": r.tags[:5],
        }
        for r in request.additionalRecipes
    ] if request.additionalRecipes else []

    return f"""
You are an elite nutritionist AI. Your ultimate goal is to compose the absolute best, most DIVERSE COMBINATION of recipes for each day.
Calculate protein/carbs/fat for every item (scaled by servings) and sum them up per slot and per day.

User profile (Health, Goals, and Diet):
{json.dumps(profile.model_dump(), indent=2)}

MAIN Meal Candidates (5 options per slot, select ONE best main per slot):
Each candidate has caloriesPerServing. Scale protein/carbs/fat proportionally by servings.
{json.dumps(days_context, indent=2)}

=== ADDITIONAL LIGHT OPTIONS (optional, ≤250 kcal each, add ONE per slot if budget allows) ===
These are low-calorie snacks, drinks, yogurts, fruits etc. filtered for this user.
You MUST add AT LEAST 3 of these additional items (distribute them across different slots, e.g., morning yogurt, juice with lunch, side salad for dinner).
Do NOT leave the day without at least 3 side items unless the main meals completely exhaust the strict calorie budget.
These have protein_g / carbs_g / fat_g per serving already provided.
Add only when you are sure that it will perfectly complement the main dish.
{json.dumps(additional_context, indent=2)}

Return STRICT JSON ONLY — no text outside JSON, no markdown block ticks. Format:
 
{{
  "days": [
    {{
      "day": 1,
      "dailyCalorieTarget": {request.dailyCalorieTarget},
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
          "side": {{
            ....
        }}
      ],
      "dayTotalCalories": 1980,
      "dailyProteinG": 105.0,
      "dailyCarbsG": 240.0,
      "dailyFatG": 58.0,
      "notes": "3-4 sentences explaining why this day fits the user's specific conditions and goals."
    }}
  ]
}}

Rules for Selection:
1. COMPOSE THE PERFECT DAY: You MUST pick the combination of meals that together hit the daily targets perfectly.
2. MACRO BOUNDARIES: The SUM of the macros for ALL chosen recipes in a single day MUST stay within the provided "minG" and "maxG" bounds.
3. USE THE SIDES POOL: To create realistic and satisfying meals, you are STRONGLY ENCOURAGED to add 1 or 2 items from the "ADDITIONAL SIDES POOL" to the "sideRecipes" list for Lunch and Dinner (e.g., combining a Meat Main + Salad Side, or Soup Main + Beverage). Do not leave "sideRecipes" empty for Lunch and Dinner unless the main recipe already perfectly perfectly hits the strict macro targets on its own.
4. VALID OPTIONS ONLY: The "mainRecipe" ID must come from the specific slot's candidates. The "sideRecipes" IDs must come from the ADDITIONAL SIDES POOL. Do not invent IDs.
5. MINIMIZE REPETITION: Aim for maximum variety across the entire week.
6. STRICT COMPLETENESS (CRITICAL): You MUST process and return ALL 7 DAYS. DO NOT stop early. DO NOT skip any days. For every single day, you MUST return ALL the slots (breakfast, lunch, dinner, snack, etc.) exactly as they were provided in the input context. Skipping days or meals is strictly forbidden.
- slotTotalCalories = main.estimatedCalories + (side.estimatedCalories if side else 0)
- slotTotalCalories MUST be ≤ calorieBudget
- dayTotalCalories = sum of all slotTotalCalories (must be within ±15% of dailyCalorieTarget)
- side can be null if budget is tight or no suitable option exists
- servings can be fractional (0.5, 1.5) to fit the calorie budget
- portionNote must be specific: grams or household measure (cups, tablespoons, pieces)
"""

# --- Llama Prompt Builders (Optimized/Compressed) ---
def _build_llama_slot_candidates(slot: SlotItem, top_k: int = 3) -> dict:
    sorted_candidates = sorted(slot.candidates, key=lambda c: c.score, reverse=True)[:top_k]
    return {
        "type": slot.mealType,
        "budget": slot.slotCalorieBudget,
        "opts": [
            {
                "id": c.recipeId, "n": c.recipeName, "cal": round(c.scaledCalories, 1),
                "p": round(c.proteinG, 1), "c": round(c.carbsG, 1), "f": round(c.fatG, 1), "srv": round(c.recommendedServings, 2),
            }
            for c in sorted_candidates
        ]
    }

def _build_llama_finalize_prompt(request: FinalizeRequest) -> str:
    profile_dict = request.userProfile.model_dump()
    days_context = []
    for day in request.days:
        days_context.append({
            "d": day.day,
            "tgt_cal": day.dailyCalorieTarget,
            "tgt_m": {
                "p": [day.estimatedDailyMacros.get("proteinMinG", 0), day.estimatedDailyMacros.get("proteinMaxG", 0)],
                "c": [day.estimatedDailyMacros.get("carbsAbsoluteMinG", 0), day.estimatedDailyMacros.get("carbsTargetG", 0)],
                "f": [day.estimatedDailyMacros.get("fatMinG", 0), day.estimatedDailyMacros.get("fatMaxG", 0)]
            },
            "slots": [_build_llama_slot_candidates(slot) for slot in day.slots if slot.candidates]
        })

    additional_context = [
        {
            "id": r.recipeId, "n": r.name, "cal": r.calories,
            "p": round(r.proteinG, 1), "c": round(r.carbsG, 1), "f": round(r.fatG, 1)
        }
        for r in request.additionalRecipes
    ] if request.additionalRecipes else []

    # Compressed JSON without spaces
    profile_json = json.dumps(profile_dict, separators=(',', ':'))
    days_json = json.dumps(days_context, separators=(',', ':'))
    add_json = json.dumps(additional_context, separators=(',', ':'))

    return f"""
You are an elite nutritionist AI. Your ultimate goal is to compose the absolute best, most DIVERSE COMBINATION of recipes for each day.
Calculate protein/carbs/fat for every item (scaled by servings) and sum them up per slot and per day.

User profile (Health, Goals, and Diet):
{profile_json}

MAIN Meal Candidates (5 options per slot, select ONE best main per slot):
Each candidate has caloriesPerServing. Scale protein/carbs/fat proportionally by servings.
{days_json}

=== ADDITIONAL LIGHT OPTIONS (optional, ≤250 kcal each, add ONE per slot if budget allows) ===
These are low-calorie snacks, drinks, yogurts, fruits etc. filtered for this user.
You MUST add AT LEAST 3 of these additional items (distribute them across different slots, e.g., morning yogurt, juice with lunch, side salad for dinner).
Do NOT leave the day without at least 3 side items unless the main meals completely exhaust the strict calorie budget.
These have protein_g / carbs_g / fat_g per serving already provided.
Add only when you are sure that it will perfectly complement the main dish.
{add_json}

Return STRICT JSON ONLY — no text outside JSON, no markdown block ticks. Format:
 
{{
  "days": [
    {{
      "day": 1,
      "dailyCalorieTarget": {request.dailyCalorieTarget},
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
          "side": {{
            ....
        }}
      ],
      "dayTotalCalories": 1980,
      "dailyProteinG": 105.0,
      "dailyCarbsG": 240.0,
      "dailyFatG": 58.0,
      "notes": "3-4 sentences explaining why this day fits the user's specific conditions and goals."
    }}
  ]
}}

Rules for Selection:
1. COMPOSE THE PERFECT DAY: You MUST pick the combination of meals that together hit the daily targets perfectly.
2. MACRO BOUNDARIES: The SUM of the macros for ALL chosen recipes in a single day MUST stay within the provided "minG" and "maxG" bounds.
3. USE THE SIDES POOL: To create realistic and satisfying meals, you are STRONGLY ENCOURAGED to add 1 or 2 items from the "ADDITIONAL SIDES POOL" to the "sideRecipes" list for Lunch and Dinner (e.g., combining a Meat Main + Salad Side, or Soup Main + Beverage). Do not leave "sideRecipes" empty for Lunch and Dinner unless the main recipe already perfectly perfectly hits the strict macro targets on its own.
4. VALID OPTIONS ONLY: The "mainRecipe" ID must come from the specific slot's candidates. The "sideRecipes" IDs must come from the ADDITIONAL SIDES POOL. Do not invent IDs.
5. MINIMIZE REPETITION: Aim for maximum variety across the entire week.
6. STRICT COMPLETENESS (CRITICAL): You MUST process and return ALL 7 DAYS (FROM day 1 TO day 7 in answer). DO NOT stop early. DO NOT skip any days. For every single day, you MUST return ALL the slots (breakfast, lunch, dinner, snack, etc.) exactly as they were provided in the input context. Skipping days or meals is strictly forbidden.
- slotTotalCalories = main.estimatedCalories + (side.estimatedCalories if side else 0)
- slotTotalCalories MUST be ≤ calorieBudget
- dayTotalCalories = sum of all slotTotalCalories (must be within ±15% of dailyCalorieTarget)
- side can be null if budget is tight or no suitable option exists
- servings can be fractional (0.5, 1.5) to fit the calorie budget
- portionNote must be specific: grams or household measure (cups, tablespoons, pieces)
"""


# ─────────────────────────────────────────────
# Models & Fallback Pipeline
# ─────────────────────────────────────────────

class MealItem(BaseModel):
    recipeId: int
    name: str
    servings: float
    estimatedCalories: float
    portionNote: str
    proteinG: float = 0.0
    carbsG: float   = 0.0
    fatG: float     = 0.0

class FinalizedSlot(BaseModel):
    mealType: str
    calorieBudget: int
    main: MealItem
    side: MealItem | None = None
    slotTotalCalories: float
    slotProteinG: float = 0.0
    slotCarbsG: float   = 0.0
    slotFatG: float     = 0.0

class FinalizedDayV3(BaseModel):
    day: int
    dailyCalorieTarget: int
    slots: list[FinalizedSlot]
    dayTotalCalories: float
    dailyProteinG: float = 0.0
    dailyCarbsG: float   = 0.0
    dailyFatG: float     = 0.0
    notes: str

class FinalizeResponseV3(BaseModel):
    userId: str
    days: list[FinalizedDayV3]


def generate_json_with_fallback(
        system_prompt: str,
        user_prompt: str,
        fallback_system_prompt: str | None = None,
        fallback_user_prompt: str | None = None,
        max_tokens: int = 14000
) -> dict:

    # Використовуємо резервні промпти, якщо вони передані, інакше основні
    llama_sys_prompt = fallback_system_prompt if fallback_system_prompt else system_prompt
    llama_user_prompt = fallback_user_prompt if fallback_user_prompt else user_prompt

    if gemini_client:
        try:
            response = gemini_client.models.generate_content(
                model=MODEL,
                contents=user_prompt,
                config=types.GenerateContentConfig(
                    system_instruction=system_prompt,
                    temperature=0.2,
                    max_output_tokens=max_tokens,
                    response_mime_type="application/json",
                )
            )
            raw = response.text
            if raw.startswith("```"):
                lines = raw.splitlines()
                raw = "\n".join(lines[1:-1] if lines[-1] == "```" else lines[1:])
            return json.loads(raw)
        except Exception as e2:
            print(f"[Fallback] Gemini failed: {e2}. Trying Llama 70B with optimized prompt...")

    try:
        response = groq_client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": llama_sys_prompt},
                {"role": "user", "content": llama_user_prompt}
            ],
            response_format={"type": "json_object"},
            temperature=0.2
        )
        raw = response.choices[0].message.content
        if raw.startswith("```"):
            lines = raw.splitlines()
            raw = "\n".join(lines[1:-1] if lines[-1] == "```" else lines[1:])
        return json.loads(raw)
    except Exception as e1:
        print(f"[Fallback] Llama 70B failed: {e1}. Trying Llama 8b...")

    try:
        response = groq_client.chat.completions.create(
            model="llama-3.1-8b-instant",
            messages=[
                {"role": "system", "content": llama_sys_prompt},
                {"role": "user", "content": llama_user_prompt}
            ],
            response_format={"type": "json_object"},
            temperature=0.2
        )
        raw = response.choices[0].message.content
        if raw.startswith("```"):
            lines = raw.splitlines()
            raw = "\n".join(lines[1:-1] if lines[-1] == "```" else lines[1:])
        return json.loads(raw)
    except Exception as e3:
        raise Exception(f"CRITICAL: All AI models failed. Last error: {e3}")


@app.post("/finalize", response_model=FinalizeResponseV3)
def finalize_meal_plan(request: FinalizeRequest):
    if not GROQ_API_KEY and not GEMINI_API_KEY:
        raise HTTPException(status_code=500, detail="No API Keys configured for LLMs")

    # Генеруємо обидві версії промптів
    gemini_prompt = _build_gemini_finalize_prompt(request)
    llama_prompt = _build_llama_finalize_prompt(request)

    try:
        parsed = generate_json_with_fallback(
            system_prompt=GEMINI_FINALIZE_SYSTEM_PROMPT,
            user_prompt=gemini_prompt,
            fallback_system_prompt=LLAMA_FINALIZE_SYSTEM_PROMPT,
            fallback_user_prompt=llama_prompt,
            max_tokens=14000
        )
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))

    finalized_days = []
    for day_data in parsed.get("days", []):
        slots = []
        for slot_data in day_data.get("slots", []):
            def to_meal_item(key):
                m = slot_data.get(key)
                if not m:
                    return None

                recipe_id = m.get("recipeId") or m.get("id")
                est_cals = m.get("estimatedCalories") or m.get("calories") or m.get("scaledCalories") or m.get("cal") or 0

                if recipe_id is None:
                    raise ValueError(f"Missing recipeId or id in {m}")

                return MealItem(
                    recipeId=recipe_id,
                    name=m.get("name") or m.get("n", "Unknown"),
                    servings=m.get("servings") or m.get("srv", 1.0),
                    estimatedCalories=est_cals,
                    portionNote=m.get("portionNote", ""),
                    proteinG=m.get("proteinG") or m.get("p", 0.0),
                    carbsG=m.get("carbsG") or m.get("c", 0.0),
                    fatG=m.get("fatG") or m.get("f", 0.0),
                )

            main_item = to_meal_item("main")
            side_item = to_meal_item("side")

            calc_cals = (main_item.estimatedCalories if main_item else 0.0) + (side_item.estimatedCalories if side_item else 0.0)
            calc_prot = (main_item.proteinG if main_item else 0.0) + (side_item.proteinG if side_item else 0.0)
            calc_carbs = (main_item.carbsG if main_item else 0.0) + (side_item.carbsG if side_item else 0.0)
            calc_fat = (main_item.fatG if main_item else 0.0) + (side_item.fatG if side_item else 0.0)

            slots.append(FinalizedSlot(
                mealType=slot_data["mealType"],
                calorieBudget=slot_data.get("calorieBudget", 0),
                main=main_item,
                side=side_item,
                slotTotalCalories=round(calc_cals, 1),
                slotProteinG=round(calc_prot, 1),
                slotCarbsG=round(calc_carbs, 1),
                slotFatG=round(calc_fat, 1),
            ))

        finalized_days.append(FinalizedDayV3(
            day=day_data.get("day", day_data.get("d", 0)),
            dailyCalorieTarget=day_data.get("dailyCalorieTarget", request.dailyCalorieTarget),
            slots=slots,
            dayTotalCalories=day_data.get("dayTotalCalories", 0),
            dailyProteinG=day_data.get("dailyProteinG", 0.0),
            dailyCarbsG=day_data.get("dailyCarbsG", 0.0),
            dailyFatG=day_data.get("dailyFatG", 0.0),
            notes=day_data.get("notes", ""),
        ))

    return FinalizeResponseV3(userId=request.userId, days=finalized_days)

REDIS_HOST = os.environ.get("REDIS_HOST", "localhost")
REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))
REDIS_DB   = int(os.environ.get("REDIS_DB", 1))
REDIS_TTL  = 60 * 60 * 24 * 30

redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB, decode_responses=True)

FOOD_PARSE_SYSTEM_PROMPT = """You are a nutrition database expert.
Parse the user's food intake description and return accurate nutritional estimates.

Rules:
- Estimate based on standard serving sizes and common preparation methods.
- If quantity is not specified, assume 1 standard serving.
- confidence: "high" (well-known food), "medium" (common dish), "low" (vague description).
- Always return valid JSON. No markdown. No text outside JSON."""

FOOD_PARSE_USER_TEMPLATE = """Parse this food intake and estimate nutrition per item.

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


# ── Cache key: нормалізована назва без кількості ──
def make_cache_key(food_name: str) -> str:
    """
    Нормалізуємо назву для кешування:
    'Hawaiian Pizza' → 'hawaiian_pizza'
    Кількість не входить у ключ — кешуємо нутрієнти на 1 одиницю.
    """
    normalized = food_name.lower().strip()
    normalized = re.sub(r'[^a-z0-9\s]', '', normalized)
    normalized = re.sub(r'\s+', '_', normalized)
    return f"food_nutrition:{normalized}"


def get_cached_nutrition(food_name: str) -> dict | None:
    key = make_cache_key(food_name)
    cached = redis_client.get(key)
    if cached:
        return json.loads(cached)
    return None

def safe_float(val):
    return float(val) if val is not None else 0.0

def cache_nutrition(food_name: str, nutrition: dict):
    key = make_cache_key(food_name)
    redis_client.setex(key, REDIS_TTL, json.dumps(nutrition, ensure_ascii=False))


# ── Pydantic models ──
class ParseFoodRequest(BaseModel):
    text: str                    # "3 куска гавайської піци та апельсиновий сік"
    language: str = "auto"       # для майбутньої локалізації


class FoodItem(BaseModel):
    name: str
    original: str
    quantity_description: str
    calories: float
    protein_g: float
    carbs_g: float
    fat_g: float
    confidence: str              # high | medium | low
    from_cache: bool = False


class ParseFoodResponse(BaseModel):
    items: list[FoodItem]
    total_calories: float
    total_protein_g: float
    total_carbs_g: float
    total_fat_g: float
    parse_note: str | None = None


# ── Endpoint ──
@app.post("/parse-food", response_model=ParseFoodResponse)
def parse_food(request: ParseFoodRequest):
    """
    Приймає довільний текст опису їжі.
    Повертає список страв з нутрієнтами.

    Спочатку перевіряємо Redis для кожної страви.
    Якщо cache miss — запитуємо Gemini і кешуємо результат.
    """
    if not GEMINI_API_KEY:
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY not set")
    if not request.text.strip():
        raise HTTPException(status_code=400, detail="Food text cannot be empty")

    client = genai.Client(api_key=GEMINI_API_KEY)

    # Крок 1: Gemini парсить текст → список страв з нутрієнтами
    prompt = FOOD_PARSE_USER_TEMPLATE.format(text=request.text.strip())

    response = client.models.generate_content(
        model=MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(
            system_instruction=FOOD_PARSE_SYSTEM_PROMPT,
            temperature=0.1,
            max_output_tokens=8000,
            response_mime_type="application/json",
        )
    )

    raw = response.text.strip()
    if raw.startswith("```"):
        lines = raw.splitlines()
        raw = "\n".join(lines[1:-1] if lines[-1] == "```" else lines[1:])

    parsed = json.loads(raw)

    # Крок 2: для кожної страви — перевіряємо кеш і оновлюємо якщо треба
    items = []
    total_calories = 0.0
    total_protein  = 0.0
    total_carbs    = 0.0
    total_fat      = 0.0

    for item_data in parsed.get("items", []):
        food_name = item_data.get("name", "Unknown food")
        calories_val = safe_float(item_data.get("calories"))
        protein_val  = safe_float(item_data.get("protein_g"))
        carbs_val    = safe_float(item_data.get("carbs_g"))
        fat_val      = safe_float(item_data.get("fat_g"))

        from_cache = False
        cached = get_cached_nutrition(food_name)
        if cached:
            # Є в кеші — беремо нутрієнти звідти
            # (кеш зберігає нутрієнти на 1 unit; тут Gemini вже порахував порцію)
            from_cache = True
        else:
            # Немає в кеші — зберігаємо що повернув Gemini
            nutrition_to_cache = {
                "calories":  calories_val,
                "protein_g": protein_val,
                "carbs_g":   carbs_val,
                "fat_g":     fat_val,
                "quantity":  item_data.get("quantity_description", "1 serving"),
            }
            cache_nutrition(food_name, nutrition_to_cache)

        food_item = FoodItem(
            name=food_name,
            original=item_data.get("original", food_name),
            quantity_description=item_data.get("quantity_description", "1 serving"),
            calories=calories_val,     # Use the sanitized value
            protein_g=protein_val,     # Use the sanitized value
            carbs_g=carbs_val,         # Use the sanitized value
            fat_g=fat_val,             # Use the sanitized value
            confidence=item_data.get("confidence", "medium"),
            from_cache=from_cache,
        )
        items.append(food_item)

        total_calories += food_item.calories
        total_protein  += food_item.protein_g
        total_carbs    += food_item.carbs_g
        total_fat      += food_item.fat_g

    return ParseFoodResponse(
        items=items,
        total_calories=round(total_calories, 1),
        total_protein_g=round(total_protein, 1),
        total_carbs_g=round(total_carbs, 1),
        total_fat_g=round(total_fat, 1),
        parse_note=parsed.get("parse_note"),
    )


@app.get("/food-cache/{food_name}")
def get_food_cache(food_name: str):
    cached = get_cached_nutrition(food_name)
    if cached:
        return {"food": food_name, "cached": True, "nutrition": cached}
    return {"food": food_name, "cached": False}
