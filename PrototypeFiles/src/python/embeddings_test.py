import json
import time
import os
from google import genai
from google.genai import types

# ─────────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────────
GEMINI_API_KEY       = os.environ.get("GEMINI_API_KEY", "YOUR_API_KEY_HERE")
MODEL                = "gemini-3.1-flash-lite-preview"
BATCH_SIZE           = 35      # інгредієнтів за один запит
DELAY_BETWEEN_BATCHES = 7       # секунд між батчами (rate limit)
SAVE_EVERY_N_BATCHES = 5        # зберігати прогрес кожні N батчів

INPUT_INGREDIENTS    = "resources/normalized_ingredients.json"
INPUT_DIETARY_REF    = "resources/dietary_reference.json"
OUTPUT_FILE          = "resources/ingredient_tags.json"
PROGRESS_FILE        = "resources/ingredient_tags_progress.json"

# ─────────────────────────────────────────────
# SYSTEM PROMPT — правила класифікації
# ─────────────────────────────────────────────
SYSTEM_PROMPT = """You are a medical nutrition expert. Your task is to classify food ingredients according to dietary restrictions and health conditions.

For each ingredient, assign one of three statuses for EVERY diet/condition:
- "allowed"        — safe to consume
- "soft_forbidden" — should be limited or consumed with caution; use this also when ingredient is ambiguous
- "hard_forbidden"  — must be strictly avoided

CLASSIFICATION RULES:

MEDICAL CONDITIONS:
- gastritis: hard_forbidden=[alcohol, coffee, caffeine, spicy peppers, chili, hot sauce, raw onion, raw garlic, vinegar, citrus juice, tomato sauce, fried foods, fatty meats]; soft_forbidden=[acidic foods, black pepper, carbonated drinks, chocolate, mint]
- diabetes: hard_forbidden=[white sugar, honey, corn syrup, maple syrup, candy, soda, white bread, white rice, pastry, cake, cookies]; soft_forbidden=[high-GI fruits (banana, mango, grapes), starchy vegetables, whole grains, natural fruit juice]; allowed=[non-starchy vegetables, lean protein, healthy fats, low-GI fruits]
- hypertension: hard_forbidden=[table salt, soy sauce, fish sauce, processed meats, bacon, ham, canned soups, pickles]; soft_forbidden=[cheese, bread, shellfish, canned foods]; allowed=[fresh vegetables, fruits, whole grains, lean proteins]
- high_cholesterol: hard_forbidden=[lard, butter, ghee, cream, full-fat cheese, organ meats, egg yolk, coconut oil, palm oil, fried foods]; soft_forbidden=[red meat, poultry skin, whole milk, shrimp]; allowed=[vegetables, fruits, fish, olive oil, nuts, oats]
- celiac_disease: hard_forbidden=[wheat, barley, rye, spelt, kamut, wheat flour, bread, pasta, beer, soy sauce (regular), most processed foods with gluten]; soft_forbidden=[oats (cross-contamination risk)]; allowed=[rice, corn, potato, quinoa, certified gluten-free products]
- lactose_intolerance: hard_forbidden=[milk, cream, ice cream, soft cheese, yogurt, butter, whey]; soft_forbidden=[hard aged cheese, ghee, lactose-free milk if intolerance is severe]; allowed=[plant milks, dairy-free products, lactose-free dairy]
- nut_allergy: hard_forbidden=[all tree nuts (almond, walnut, cashew, pecan, hazelnut, pistachio, macadamia, brazil nut), peanut, nut butter, nut oil, marzipan, praline]; soft_forbidden=[coconut (sometimes classified as tree nut), seeds processed near nuts]
- shellfish_allergy: hard_forbidden=[shrimp, crab, lobster, clam, oyster, scallop, mussel, crawfish, squid, octopus]; soft_forbidden=[fish sauce, oyster sauce, worcestershire sauce (may contain)]
- fish_allergy: hard_forbidden=[all fish species, fish sauce, anchovy, anchovy paste, worcestershire sauce, caesar dressing]; soft_forbidden=[anything processed in fish facilities]
- kidney_disease: hard_forbidden=[high-potassium foods (banana, orange, potato, tomato), high-phosphorus foods (dairy, nuts, seeds, cola), high-sodium foods, organ meats]; soft_forbidden=[whole grains, legumes, red meat]; allowed=[white rice, white bread, cabbage, apple, berries]
- gout: hard_forbidden=[organ meats (liver, kidney, sweetbread), anchovies, sardines, herring, mackerel, shellfish, beer, alcohol, red meat]; soft_forbidden=[chicken, pork, beef, fish (other), spinach, mushroom, asparagus, cauliflower, legumes]; allowed=[dairy, eggs, vegetables (most), fruits, coffee]
- pancreatitis: hard_forbidden=[alcohol, fried foods, fatty meats, cream, full-fat cheese, butter, lard, coconut oil, fast food]; soft_forbidden=[red meat, egg yolk, avocado, nuts]; allowed=[lean chicken, fish, vegetables, fruits, low-fat dairy, whole grains]
- gerd: hard_forbidden=[coffee, alcohol, chocolate, mint, peppermint, tomato sauce, citrus juice, spicy foods, fried foods, onion, garlic]; soft_forbidden=[carbonated drinks, high-fat foods, vinegar, citrus fruits]; allowed=[oatmeal, ginger, vegetables, lean proteins, non-citrus fruits]
- ibs: hard_forbidden=[high-FODMAP: garlic, onion, wheat, rye, milk, soft cheese, apple, pear, mango, stone fruits, beans, lentils, cauliflower, mushroom, chicory]; soft_forbidden=[caffeine, alcohol, spicy foods, fatty foods]; allowed=[low-FODMAP: rice, oats, carrot, zucchini, spinach, banana (unripe), blueberry, strawberry, lactose-free dairy, hard cheese]

DIETS:
- vegetarian: hard_forbidden=[beef, pork, chicken, turkey, lamb, veal, duck, goose, venison, fish, shellfish, seafood, gelatin, lard, meat broth, bone broth]; soft_forbidden=[rennet cheese, some wines with gelatin fining]; allowed=[eggs, dairy, vegetables, fruits, grains, legumes, nuts, seeds]
- vegan: hard_forbidden=[all meat, fish, seafood, eggs, milk, cream, cheese, butter, honey, gelatin, lard, whey, casein, collagen, bone broth, beeswax]; soft_forbidden=[some wines, some sugar (bone char processed)]; allowed=[all plants, vegetables, fruits, grains, legumes, nuts, seeds, plant milks]
- keto: hard_forbidden=[sugar, bread, pasta, rice, potato, corn, most fruits, beans, legumes, oats, most grains]; soft_forbidden=[some dairy (too much protein), high-carb vegetables (carrot, beet), alcohol]; allowed=[meat, fish, eggs, cheese, butter, cream, low-carb vegetables, nuts, seeds, olive oil, avocado]
- paleo: hard_forbidden=[grains (wheat, rice, oats, corn), legumes (beans, peanuts, soy), dairy, refined sugar, salt, processed foods, vegetable oils]; soft_forbidden=[white potato, alcohol, coffee]; allowed=[meat, fish, eggs, vegetables, fruits, nuts, seeds, natural fats, herbs, spices]
- mediterranean: soft_forbidden=[red meat (limit), butter, cream, processed foods, refined sugar, refined grains]; allowed=[olive oil, fish, seafood, vegetables, fruits, whole grains, legumes, nuts, seeds, moderate wine, moderate dairy]
- low_calorie: hard_forbidden=[sugar, candy, chips, fried foods, full-fat cheese, cream, butter, oil (large amounts), alcohol]; soft_forbidden=[bread, pasta, rice, starchy vegetables, dried fruits, nuts (calorie-dense)]; allowed=[vegetables (non-starchy), lean protein, low-fat dairy, fruits (fresh)]
- gluten_free: hard_forbidden=[wheat, barley, rye, spelt, farro, kamut, wheat flour, bread (regular), pasta (regular), beer, soy sauce (regular), seitan]; soft_forbidden=[oats (cross-contamination), malt vinegar, some processed foods]; allowed=[rice, corn, potato, quinoa, buckwheat, certified gluten-free products, meat, fish, eggs, dairy, vegetables, fruits]

IMPORTANT RULES:
1. If ingredient is ambiguous (e.g., "cream" could be dairy or cooking cream) → use "soft_forbidden" where doubt exists
2. Classify the ingredient AS-IS — do not assume preparation method
3. For compound ingredients (e.g., "bean with bacon soup") — classify based on the most restrictive component
4. Be consistent across the batch

Respond ONLY with a valid JSON object. No markdown, no explanation, no backticks.
Format:
{
  "ingredient_name": {
    "gastritis": "allowed",
    "diabetes": "soft_forbidden",
    ...all 21 diet/condition ids...
  },
  ...
}"""

# ─────────────────────────────────────────────
# USER PROMPT для батчу
# ─────────────────────────────────────────────
def build_user_prompt(batch: list[str]) -> str:
    ingredients_json = json.dumps(batch, ensure_ascii=False)
    return f"""Classify these ingredients. Return JSON only.

Ingredients to classify:
{ingredients_json}

Required keys for each ingredient (all 21):
gastritis, diabetes, hypertension, high_cholesterol, celiac_disease,
lactose_intolerance, nut_allergy, shellfish_allergy, fish_allergy,
kidney_disease, gout, pancreatitis, gerd, ibs,
vegetarian, vegan, keto, paleo, mediterranean, low_calorie, gluten_free

Allowed values: "allowed", "soft_forbidden", "hard_forbidden"
"""

# ─────────────────────────────────────────────
# Gemini API call
# ─────────────────────────────────────────────
def sanitize_ingredient(name: str) -> str:
    """Прибирає зайві символи які можуть зламати JSON відповідь моделі."""
    return name.strip(" -,.'\"").strip()

def classify_batch(client: genai.Client, batch: list[str]) -> dict:
    response = client.models.generate_content(
        model=MODEL,
        contents=build_user_prompt(batch),
        config=types.GenerateContentConfig(
            system_instruction=SYSTEM_PROMPT,
            temperature=0.1,      # мінімальна варіативність для консистентності
            max_output_tokens=16000,
            response_mime_type="application/json"
        )
    )

    raw = response.text.strip()

    # Захист від markdown-обгортки якщо модель все ж додала ```json
    if raw.startswith("```"):
        lines = raw.splitlines()
        raw = "\n".join(lines[1:-1] if lines[-1] == "```" else lines[1:])

    return json.loads(raw)

# ─────────────────────────────────────────────
# Валідація результату батчу
# ─────────────────────────────────────────────
REQUIRED_KEYS = {
    "gastritis", "diabetes", "hypertension", "high_cholesterol", "celiac_disease",
    "lactose_intolerance", "nut_allergy", "shellfish_allergy", "fish_allergy",
    "kidney_disease", "gout", "pancreatitis", "gerd", "ibs",
    "vegetarian", "vegan", "keto", "paleo", "mediterranean", "low_calorie", "gluten_free"
}
VALID_STATUSES = {"allowed", "soft_forbidden", "hard_forbidden"}

def validate_result(result: dict, batch: list[str]) -> tuple[dict, list[str]]:
    """Повертає (валідні записи, список інгредієнтів що потрібно повторити)."""
    valid = {}
    retry = []

    for ingredient in batch:
        if ingredient not in result:
            retry.append(ingredient)
            continue

        tags = result[ingredient]
        missing_keys = REQUIRED_KEYS - set(tags.keys())
        invalid_vals = [k for k, v in tags.items() if v not in VALID_STATUSES]

        if missing_keys or invalid_vals:
            print(f"  ⚠️  Невалідний запис для '{ingredient}': "
                  f"missing={missing_keys}, invalid={invalid_vals}")
            retry.append(ingredient)
        else:
            valid[ingredient] = tags

    return valid, retry

# ─────────────────────────────────────────────
# Завантаження прогресу
# ─────────────────────────────────────────────
def load_progress() -> dict:
    if os.path.exists(PROGRESS_FILE):
        with open(PROGRESS_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
        print(f"🔄 Продовжуємо з прогресу: {len(data)} інгредієнтів вже класифіковано")
        return data
    return {}

def save_progress(results: dict):
    with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

# ─────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────
def main():
    # Завантаження даних
    with open(INPUT_INGREDIENTS, "r", encoding="utf-8") as f:
        all_ingredients = json.load(f)

    print(f"📦 Всього інгредієнтів: {len(all_ingredients)}")

    # Санітизація — прибираємо сміттєві символи з країв
    all_ingredients = [sanitize_ingredient(i) for i in all_ingredients]
    all_ingredients = [i for i in all_ingredients if i]  # прибираємо порожні після санітизації
    all_ingredients = sorted(set(all_ingredients))        # дедуплікація після санітизації
 
    print(f"📦 Всього інгредієнтів після санітизації: {len(all_ingredients)}")

    # Відновлення прогресу якщо є
    results = load_progress()
    already_done = set(results.keys())

    # Фільтруємо вже оброблені
    remaining = [i for i in all_ingredients if i not in already_done]
    print(f"⏳ Залишилось класифікувати: {len(remaining)}")

    if not remaining:
        print("✅ Всі інгредієнти вже класифіковано!")
        return

    # Ініціалізація клієнта
    client = genai.Client(api_key=GEMINI_API_KEY)

    # Розбивка на батчі
    batches = [remaining[i:i+BATCH_SIZE] for i in range(0, len(remaining), BATCH_SIZE)]
    total_batches = len(batches)

    print(f"🚀 Запускаємо класифікацію: {total_batches} батчів по ~{BATCH_SIZE} інгредієнтів\n")

    for batch_idx, batch in enumerate(batches):
        print(f"[{batch_idx+1}/{total_batches}] Обробка {len(batch)} інгредієнтів...", end=" ")

        retry_count = 0
        current_batch = batch

        while current_batch and retry_count < 3:
            try:
                raw_result = classify_batch(client, current_batch)
                valid, to_retry = validate_result(raw_result, current_batch)
                results.update(valid)

                if to_retry:
                    print(f"\n  🔁 Повтор для {len(to_retry)} інгредієнтів...")
                    current_batch = to_retry
                    retry_count += 1
                    time.sleep(7)
                else:
                    current_batch = []

            except json.JSONDecodeError as e:
                print(f"\n  ❌ JSON parse error (спроба {retry_count+1}/3): {e}")
                retry_count += 1
                time.sleep(7)

            except Exception as e:
                print(f"\n  ❌ API error (спроба {retry_count+1}/3): {e}")
                retry_count += 1
                time.sleep(7)

        if current_batch:
            print(f"\n  ⚠️  Пропускаємо {len(current_batch)} інгредієнтів після 3 спроб: {current_batch}")

        print(f"✅ Всього класифіковано: {len(results)}/{len(all_ingredients)}")

        # Зберігаємо прогрес
        if (batch_idx + 1) % SAVE_EVERY_N_BATCHES == 0:
            save_progress(results)
            print(f"💾 Прогрес збережено")

        # Rate limit пауза
        if batch_idx < total_batches - 1:
            time.sleep(DELAY_BETWEEN_BATCHES)

    # Фінальне збереження
    save_progress(results)

    # Зберігаємо фінальний відсортований результат
    sorted_results = dict(sorted(results.items()))
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(sorted_results, f, indent=2, ensure_ascii=False)

    print(f"\n🎉 Готово!")
    print(f"   Класифіковано: {len(sorted_results)}/{len(all_ingredients)} інгредієнтів")
    print(f"   Файл збережено: {OUTPUT_FILE}")

    # Статистика пропущених
    missed = set(all_ingredients) - set(sorted_results.keys())
    if missed:
        print(f"   ⚠️  Не класифіковано: {len(missed)} інгредієнтів")
        print(f"   Список: {sorted(missed)}")


if __name__ == "__main__":
    main()