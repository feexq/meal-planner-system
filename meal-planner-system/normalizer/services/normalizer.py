import re
import urllib.parse

import inflect

# ─── Regex helpers ────────────────────────────────────────────────────────────
_QUANTITY_RE   = re.compile(r'\b\d+[\d/\.\s]*%?(?!-[a-zA-Z])|%\d+|\d+%25|%25|\b\d+[\.\d]*\s*(?!-[a-zA-Z])')
_SIZE_RE       = re.compile(r'\b\d+[\-\s]?(?:inch|in\b|cm\b|mm\b|quart\b|qt\b)')
_PAREN_RE      = re.compile(r'\(.*?\)')
_SPACE_RE      = re.compile(r'\s+')
_TRADEMARK_RE  = re.compile(r'&reg;|&amp;|®|™', re.IGNORECASE)
_BRAND_NAME_RE = re.compile(
    r'\b(pillsbury\s+best|pillsbury|crisco|bisquick|jell-?o|kraft|heinz|'
    r'campbells?|hidden\s+valley|rosarita|ro\s*tel|market\s+pantry|'
    r'simply\s+potatoes?|swanson|velveeta|philadelphia|hellmann\'?s|'
    r'lipton|progresso|betty\s+crocker|land\s+o\s+lakes)\b',
    re.IGNORECASE,
)
_TAIL_RE = re.compile(
    r'\s+(?:packed\s+in|in|with)\s+'
    r'(?:water|brine|puree|oil|vegetable\s+oil|tomato\s+puree|tomato\s+juice)$',
    re.IGNORECASE,
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
    "ready-to-serve", "ready-to-bake", "quick-cooking",
}

REPAIR_MAP = {
    "asparagu": "asparagus",
    "couscou":  "couscous",
    "hummu":    "hummus",
    "cachou":   "cachous",
    "watercres":"watercress",
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

_inflect = inflect.engine()


def _strip_brands(text: str) -> str:
    text = _TRADEMARK_RE.sub("", text)
    text = _BRAND_NAME_RE.sub("", text)
    return text.strip(" -,.'\"")


def _strip_tail(text: str) -> str:
    if text in KEEP_TAIL_PHRASES:
        return text
    return _TAIL_RE.sub("", text).strip()


def _safe_singularize(word: str) -> str:
    if word in NO_SINGULARIZE:
        return word
    result = _inflect.singular_noun(word)
    return result if result else word


def _clean_descriptors(words: list[str]) -> list[str]:
    out = []
    for w in words:
        if w in STRIP_WORDS:
            continue
        if len(w) == 1 and w not in ("a", "i"):
            continue
        if re.match(r"^[\d%\-\+]+$", w):
            continue
        out.append(w)
    return out


def normalize_ingredient_text(text: str) -> str:
    """Full pipeline: decode → strip → singularize → clean."""
    text = urllib.parse.unquote(text).lower().strip()

    if text in REPAIR_MAP:
        return REPAIR_MAP[text]

    words = text.split()
    if words and words[-1] in REPAIR_MAP:
        words[-1] = REPAIR_MAP[words[-1]]
        text = " ".join(words)

    text = _strip_brands(text)
    if not text:
        return ""

    parts     = [p.strip() for p in text.split(",")]
    main_part = parts[0]
    desc      = " ".join(parts[1:]) if len(parts) > 1 else ""

    for attr in ("main_part", "desc"):
        val = locals()[attr]
        val = _PAREN_RE.sub("", val)
        val = _SIZE_RE.sub("", val)
        val = _QUANTITY_RE.sub("", val)
        val = _SPACE_RE.sub(" ", val).strip()
        if attr == "main_part":
            main_part = val
        else:
            desc = val

    final = f"{main_part} {desc}".strip()
    final = _strip_tail(final)

    words = _clean_descriptors(final.split())
    result = []
    for w in words:
        if w in ("of", "zest", "juice", "peel"):
            result.append(w)
        else:
            result.append(_safe_singularize(w))

    return " ".join(result)