import json
import time
import os
from google import genai
from google.genai import types

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "YOUR_API_KEY_HERE")

MODEL = "gemini-3.1-flash-lite-preview"

BATCH_SIZE = 50
DELAY_BETWEEN_BATCHES = 6
SAVE_EVERY_N_BATCHES = 5

INPUT_FILE = "resources/recipes_extended_fixed2.json"
OUTPUT_FILE = "resources/recipes_classified_final.json"
PROGRESS_FILE = "resources/recipes_progress.json"

ALLOWED_MEAL_TYPES = [
    "breakfast",
    "lunch",
    "dinner",
    "dessert",
    "snack",
    "drink",
    "sauce_or_condiment",
    "unclassified"
]

SYSTEM_PROMPT = f"""
You are an expert culinary AI. I will give you a JSON list of recipes.
Analyze the name, description, ingredients, and steps for each recipe.
Classify each recipe into EXACTLY ONE of the following meal types: {", ".join(ALLOWED_MEAL_TYPES)}.

Category definitions:

breakfast:
Foods typically eaten in the morning such as eggs, pancakes, oatmeal, breakfast sandwiches.

lunch:
A complete meal typically eaten in the middle of the day.
Examples include sandwiches, salads with protein, rice bowls, pasta dishes, soups, or leftovers from dinner.
Lunch should be a standalone meal, not just a sauce, condiment, or side component.

dinner:
A large or substantial main meal, usually heavier or more complex than lunch.
Examples include roasted meat dishes, pasta meals, casseroles, stews, or full cooked meals.

dessert:
Sweet foods eaten after meals such as cakes, cookies, brownies, ice cream.

snack:
Small foods eaten between meals such as nuts, chips, fruit snacks, small sandwiches.

drink:
Beverages such as smoothies, juices, coffee drinks, milkshakes, cocktails.

sauce_or_condiment:
Sauces, dressings, marinades, dips, condiments, spreads, or toppings used to accompany other dishes rather than eaten as a standalone meal.

Examples:
- salad dressing
- pasta sauce
- marinade
- dipping sauce
- salsa
- mayonnaise
- ketchup
- gravy
- aioli
- pesto

unclassified:
Recipes that cannot clearly be categorized into any of the above groups.
Use this if the recipe is unclear, incomplete, or does not represent a meal, drink, or condiment.

Rules:

1. Choose EXACTLY ONE category.
2. Do NOT invent new categories.
3. If the recipe is a beverage → classify as "drink".
4. If it is primarily a sweet treat eaten after a meal -> classify as 'dessert'. (Do not confuse with sweet breakfasts or sweet drinks)."
5. If it is a sauce, marinade, dressing, dip, or condiment → classify as "sauce_or_condiment".
6. If it is a large, heavy main dish clearly meant for evening -> classify as 'dinner'. If it's a lighter full meal -> classify as 'lunch'.
7. If it is small and eaten between meals → classify as "snack".
8. If the recipe does not clearly fit any category → classify as "unclassified".

Return JSON only.

Format:
[
  {{
    "id": 123,
    "meal_type": "dinner",
    "justification": "Short explanation"
  }}
]
"""


def build_user_prompt(batch):

    minimal_data = []

    for r in batch:

        ingredients = r.get("ingredients", "")

        if isinstance(ingredients, list):
            ingredients = ", ".join(ingredients)

        minimal_data.append({
            "id": r["id"],
            "name": r.get("name", ""),
            "description": r.get("description", ""),
            "ingredients": ingredients,
            "steps": r.get("steps", "")
        })

    return f"""
Classify these recipes.

Important rules:
- Sauces, marinades, dressings, dips → "sauce_or_condiment"
- If the recipe does not clearly belong to any category → "unclassified"

Return JSON only.

Recipes:
{json.dumps(minimal_data, ensure_ascii=False)}
"""


# ─────────────────────────────────────────────
# Gemini API call
# ─────────────────────────────────────────────

def classify_batch(client, batch):

    response = client.models.generate_content(
        model=MODEL,
        contents=build_user_prompt(batch),
        config=types.GenerateContentConfig(
            system_instruction=SYSTEM_PROMPT,
            temperature=0.1,
            response_mime_type="application/json",
            max_output_tokens=16000
        )
    )

    raw = response.text.strip()

    # прибираємо markdown якщо модель його дала
    if raw.startswith("```"):
        lines = raw.splitlines()
        raw = "\n".join(lines[1:-1])

    return json.loads(raw)


# ─────────────────────────────────────────────
# Validation
# ─────────────────────────────────────────────

def validate_result(result, batch):

    valid = {}
    retry = []

    batch_ids = {str(r["id"]) for r in batch}

    for item in result:

        rid = str(item.get("id"))

        if rid not in batch_ids:
            continue

        meal_type = item.get("meal_type", "").lower()

        if meal_type not in ALLOWED_MEAL_TYPES:
            meal_type = "unclassified"
            continue

        valid[rid] = item

    for r in batch:
        if str(r["id"]) not in valid:
            retry.append(str(r["id"]))

    return valid, retry


# ─────────────────────────────────────────────
# Progress
# ─────────────────────────────────────────────

def load_progress():

    if os.path.exists(PROGRESS_FILE):

        with open(PROGRESS_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)

        print(f"🔄 Відновлено {len(data)} рецептів з прогресу")

        return data

    return {}


def save_progress(data):

    with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


# ─────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────

def main():

    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        all_recipes = json.load(f)

    print(f"📦 Всього рецептів: {len(all_recipes)}")

    results = load_progress()
    processed_ids = set(results.keys())

    remaining = [r for r in all_recipes if str(r["id"]) not in processed_ids]

    print(f"⏳ Залишилось класифікувати: {len(remaining)}")

    if not remaining:
        print("✅ Всі рецепти вже оброблені")
        return

    client = genai.Client(api_key=GEMINI_API_KEY)

    batches = [remaining[i:i+BATCH_SIZE] for i in range(0, len(remaining), BATCH_SIZE)]

    total_batches = len(batches)

    print(f"🚀 Запускаємо: {total_batches} батчів\n")

    for batch_idx, batch in enumerate(batches):

        print(f"[{batch_idx+1}/{total_batches}] Обробка {len(batch)} рецептів...")

        retry_count = 0
        current_batch = batch

        while current_batch and retry_count < 3:

            try:

                raw_result = classify_batch(client, current_batch)

                valid, retry = validate_result(raw_result, current_batch)

                for rid, data in valid.items():
                    results[rid] = data

                if retry:

                    retry_ids = set(retry)

                    current_batch = [
                        r for r in current_batch
                        if str(r["id"]) in retry_ids
                    ]

                    print(f"🔁 Повтор для {len(current_batch)} рецептів")

                    retry_count += 1
                    time.sleep(6)

                else:
                    current_batch = []

            except json.JSONDecodeError as e:

                print(f"❌ JSON parse error: {e}")

                retry_count += 1
                time.sleep(6)

            except Exception as e:

                print(f"❌ API error: {e}")

                retry_count += 1
                time.sleep(6)

        print(f"✅ Всього класифіковано: {len(results)}/{len(all_recipes)}")

        if (batch_idx + 1) % SAVE_EVERY_N_BATCHES == 0:
            save_progress(results)
            print("💾 Прогрес збережено")

        if batch_idx < total_batches - 1:
            time.sleep(DELAY_BETWEEN_BATCHES)

    save_progress(results)

    final = []

    results_map = {str(k): v for k, v in results.items()}

    for r in all_recipes:

        rid = str(r["id"])

        if rid in results_map:
            r["meal_type"] = results_map[rid]["meal_type"]
            r["meal_type_justification"] = results_map[rid]["justification"]

        final.append(r)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(final, f, indent=2, ensure_ascii=False)

    print("\n🎉 Готово!")
    print(f"Файл: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()