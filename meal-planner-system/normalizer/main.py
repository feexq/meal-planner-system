import os
import json
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from google import genai
from google.genai import types
import re
import inflect
import urllib.parse
from ingredient_parser import parse_ingredient

app = FastAPI()

# ── Gemini config ──
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "")
MODEL = "gemini-3.1-flash-lite-preview"

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
    # Спочатку через NLP парсер
    parsed = parse_ingredient(req.raw_name)
    
    if parsed.name and len(parsed.name) > 0:
        nlp_text = " ".join([n.text for n in parsed.name])
        normalized = deep_clean_ingredient_text(nlp_text)
    else:
        # Fallback — чистимо напряму без NLP
        normalized = deep_clean_ingredient_text(req.raw_name)

    # Якщо після всієї обробки порожньо — повертаємо оригінал
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


def classify_batch(client, batch: list[str]) -> tuple[dict, list[str]]:
    response = client.models.generate_content(
        model=MODEL,
        contents=build_user_prompt(batch),
        config=types.GenerateContentConfig(
            system_instruction=SYSTEM_PROMPT,
            temperature=0.1,
            max_output_tokens=16000,
            response_mime_type="application/json"
        )
    )

    raw = response.text.strip()
    if raw.startswith("```"):
        lines = raw.splitlines()
        raw = "\n".join(lines[1:-1] if lines[-1] == "```" else lines[1:])

    result = json.loads(raw)

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
    if not GEMINI_API_KEY:
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY not set")
    if not req.ingredients:
        return ClassifyResponse(results={}, failed=[])

    client = genai.Client(api_key=GEMINI_API_KEY)

    valid, failed = classify_batch(client, req.ingredients)

    # Retry для failed
    if failed:
        retry_valid, still_failed = classify_batch(client, failed)
        valid.update(retry_valid)
        failed = still_failed

    return ClassifyResponse(results=valid, failed=failed)