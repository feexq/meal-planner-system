import json
import ast
import re
import inflect
import urllib.parse
from ingredient_parser import parse_ingredient

# ─────────────────────────────────────────────
# 1. Regex Cleaners
# ─────────────────────────────────────────────
_QUANTITY_RE = re.compile(r'\b\d+[\d/\.\s]*%?(?!-[a-zA-Z])|%\d+|\d+%25|%25|\b\d+[\.\d]*\s*(?!-[a-zA-Z])')
_SIZE_RE     = re.compile(r'\b\d+[\-\s]?(?:inch|in\b|cm\b|mm\b|quart\b|qt\b)')
_PAREN_RE    = re.compile(r'\(.*?\)')
_SPACE_RE    = re.compile(r'\s+')

# FIX 1: Видалення брендів і trademark-символів
# ВАЖЛИВО: спочатку видаляємо &reg;/® (вони можуть стояти всередині назви бренду),
# і тільки потім застосовуємо brand regex
_TRADEMARK_RE = re.compile(r'&reg;|&amp;|®|™', re.IGNORECASE)
# Видаляємо тільки саму бренд-назву, зберігаючи решту тексту
# (наприклад: "pillsbury best all purpose flour" → "all purpose flour")
_BRAND_NAME_RE = re.compile(
    r'\b(pillsbury\s+best|pillsbury|crisco|bisquick|jell-?o|kraft|heinz|'
    r'campbells?|hidden\s+valley|rosarita|ro\s*tel|market\s+pantry|'
    r'simply\s+potatoes?|swanson|velveeta|philadelphia|hellmann\'?s|'
    r'lipton|progresso|betty\s+crocker|land\s+o\s+lakes)\b',
    re.IGNORECASE
)

# FIX 2: Видалення прийменникових хвостів "in water", "packed in oil" тощо
# olive oil НЕ видаляємо — важливо для mediterranean/cholesterol дієт
_TAIL_RE = re.compile(
    r'\s+(?:packed\s+in|in|with)\s+'
    r'(?:water|brine|puree|oil|'
    r'vegetable\s+oil|tomato\s+puree|tomato\s+juice)$',
    re.IGNORECASE
)

# Конструкції які НЕ треба чіпати — вони змінюють суть інгредієнта
# (важливо для дієтичної класифікації)
KEEP_TAIL_PHRASES = {
    # містять інший інгредієнт що впливає на дієту
    "bean with bacon soup",
    "cheese with green chili pepper",
    "cheese with jalapeno pepper",
    "cream cheese with chive and onion",
    "english muffin with raisin",
    "mutton bone with marrow",
    "pasta sauce with mushroom",
    "pork and bean in tomato sauce",
    "refried bean with green chili",
    "refried bean with jalapeno",
    "tomato with basil oregano and garlic",
    "tomato with green chili",
    "tomato with jalapeno",
    "ranch style black-eyed pea with jalapeno",
    "chipotle chile in adobo",          # adobo — окремий соус, важливий
    "bean in tomato sauce",
    "simply potato potato with onion",
    "silver cachou",
}

# ─────────────────────────────────────────────
# 2. Stop-Words (без змін — залишаємо твій список)
# ─────────────────────────────────────────────
STRIP_WORDS = {
    # Preparations & States
    "chopped", "minced", "diced", "sliced", "grated", "shredded",
    "crushed", "mashed", "pureed", "blended", "ground", "beaten",
    "softened", "melted", "cooked", "baked", "fried", "steamed",
    "frozen", "thawed", "dried", "fresh", "raw", "canned", "jarred",
    "roasted", "toasted", "boiled", "grilled", "smashed", "flaked",
    "pickled", "marinated", "soaked", "drained", "rinsed", "peeled",
    "seeded", "pitted", "trimmed", "halved", "quartered", "cubed",

    # Sizes & Amounts
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

    # Qualifiers & Descriptors
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

    # Nutritional & Dietary Hyphenates
    "low-sodium", "reduced-sodium", "no-salt", "no-salt-added", "sodium-free",
    "reduced-fat", "low-fat", "fat-free", "nonfat", "non-fat",
    "sugar-free", "gluten-free", "dairy-free", "low-carb", "low-calorie",
    "reduced-calorie", "non-dairy", "vegan", "vegetarian",

    # Meat Parts & Cuts
    "lean", "boneless", "skinless", "skin-on", "bone-in",
    "breast", "breasts", "thigh", "thighs", "wing", "wings",
    "leg", "legs", "fillet", "fillets", "cutlet", "cutlets",

    # Compound strings
    "ready-to-serve", "ready-to-bake", "quick-cooking"
}

# ─────────────────────────────────────────────
# FIX 3: Whitelist + REPAIR_MAP
# ─────────────────────────────────────────────

# inflect вже зламав ці слова в попередніх запусках — відновлюємо вручну
# застосовується як перший крок обробки
REPAIR_MAP = {
    'asparagu':  'asparagus',
    'couscou':   'couscous',
    'hummu':     'hummus',
    'cachou':    'cachous',
    'watercres': 'watercress',
}

NO_SINGULARIZE = {
    # Латинські/грецькі слова що закінчуються на -us/-is/-os
    "asparagus", "couscous", "hummus", "quinoa", "shoyu",
    "molasses", "watercress", "tahini", "tzatziki", "harissa",
    "kombucha", "tempeh", "miso", "tofu", "edamame",
    # Слова які inflect singularize некоректно
    "oats", "grits", "lentils", "greens", "herbs", "sprouts",
    "breadcrumbs", "bitters", "anchovies", "series", "species",
    # Слова де множина = однина
    "fish", "deer", "sheep", "moose",
    # Слова де singularize дає неправильний результат
    "cachou", "orzo", "panko", "pesto", "risotto", "polenta",
    "mascarpone", "prosciutto", "pancetta", "chorizo", "andouille",
    "guacamole", "salsa", "aioli", "tapenade", "chimichurri",
}

p = inflect.engine()


# ─────────────────────────────────────────────
# Helper functions
# ─────────────────────────────────────────────

def strip_brands(text):
    """FIX 1: Спочатку видалити ® між словами, потім видалити тільки бренд-назву (хвіст зберігається)."""
    text = _TRADEMARK_RE.sub('', text)   # "pillsbury best&reg; flour" → "pillsbury best flour"
    text = _BRAND_NAME_RE.sub('', text)  # "pillsbury best flour" → "flour"
    return text.strip(" -,.'\"")         # прибираємо сміття включно з апострофами/лапками


def strip_tail(text):
    """FIX 2: Видалити 'in water/oil/syrup' хвости якщо вони не несуть дієтичної інформації."""
    if text in KEEP_TAIL_PHRASES:
        return text
    return _TAIL_RE.sub('', text).strip()


def safe_singularize(word):
    """FIX 3: Сингуляризація з whitelist для проблемних слів."""
    if word in NO_SINGULARIZE:
        return word
    result = p.singular_noun(word)
    return result if result else word


def clean_descriptor_words(words):
    """Видаляє залишки одиниць, зайві літери, символи. Зберігає 'zest', 'juice', 'peel', 'of'."""
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

    # FIX 3а: Відновити слова які inflect зламав у попередніх запусках
    if text in REPAIR_MAP:
        return REPAIR_MAP[text]
    # Також перевіряємо чи зламане слово є частиною фрази
    words_check = text.split()
    if words_check and words_check[-1] in REPAIR_MAP:
        words_check[-1] = REPAIR_MAP[words_check[-1]]
        text = ' '.join(words_check)

    # FIX 1: Прибрати бренди до будь-якої іншої обробки
    text = strip_brands(text)
    if not text:
        return ''

    # Розділ по комі — перша частина основна
    parts = [pt.strip() for pt in text.split(',')]
    main_part  = parts[0]
    descriptors = ' '.join(parts[1:]) if len(parts) > 1 else ''

    # Стандартна regex-очистка
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

    # FIX 2: Прибрати нейтральні хвости (in water, in syrup тощо)
    final_text = strip_tail(final_text)

    # Розділ на слова та очистка стоп-слів
    words = final_text.split()
    words = clean_descriptor_words(words)

    # FIX 3: Сингуляризація з whitelist
    normalized_words = []
    for word in words:
        if word in ("of", "zest", "juice", "peel"):
            normalized_words.append(word)
            continue
        normalized_words.append(safe_singularize(word))

    result = ' '.join(normalized_words)
    return result


# ─────────────────────────────────────────────
# Main pipeline
# ─────────────────────────────────────────────

def extract_and_super_normalize(input_filepath, output_filepath, mapping_filepath=None):
    unique_ingredients = set()
    # raw ingredient text → нормалізована назва
    normalization_map = {}

    print(f"Loading recipes from {input_filepath}...")
    with open(input_filepath, 'r', encoding='utf-8') as file:
        recipes = json.load(file)

    total_recipes = len(recipes)

    for index, recipe in enumerate(recipes):
        if index % 1000 == 0 and index > 0:
            print(f"Processed {index}/{total_recipes} recipes...")

        ingredients_str = recipe.get('ingredients', '[]')

        try:
            ingredients_list = ast.literal_eval(ingredients_str)

            for ingredient in ingredients_list:
                # Step 1: NLP парсер витягує core name
                parsed = parse_ingredient(ingredient)

                # Step 2: Збираємо текст з парсера
                if parsed.name and len(parsed.name) > 0:
                    nlp_text = " ".join([n.text for n in parsed.name])

                    # Step 3: Агресивна чистка + фікси
                    final_clean_name = deep_clean_ingredient_text(nlp_text)

                    if final_clean_name:
                        unique_ingredients.add(final_clean_name)
                        # Зберігаємо маппінг оригінал → нормалізований
                        # Ключ — оригінальний рядок з рецепту (lowercase, trimmed)
                        raw_key = ingredient.strip().lower()
                        if raw_key not in normalization_map:
                            normalization_map[raw_key] = final_clean_name

        except (ValueError, SyntaxError):
            print(f"Skipping recipe ID {recipe.get('id')} due to parsing error.")

    final_ingredients_list = sorted(list(unique_ingredients))

    print(f"Saving {len(final_ingredients_list)} normalized ingredients to {output_filepath}...")
    with open(output_filepath, 'w', encoding='utf-8') as output_file:
        json.dump(final_ingredients_list, output_file, indent=4, ensure_ascii=False)

    # Зберігаємо маппінг якщо вказано шлях
    if mapping_filepath:
        sorted_map = dict(sorted(normalization_map.items()))
        print(f"Saving normalization map ({len(sorted_map)} entries) to {mapping_filepath}...")
        with open(mapping_filepath, 'w', encoding='utf-8') as map_file:
            json.dump(sorted_map, map_file, indent=2, ensure_ascii=False)

    print("Done!")


# Run
extract_and_super_normalize(
    'resources/recipes_classified(1).json',
    'resources/normalized_ingredients.json',
    'resources/ingredient_normalization_map.json'   # новий файл маппінгу
)