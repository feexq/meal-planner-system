import json
import requests
import re
import os
from concurrent.futures import ThreadPoolExecutor, as_completed

OUTPUT_FILE = "resources/nutrition_data_formatted.json"
NUM_WORKERS = 25


def name_to_slug(name):
    name = name.replace("&amp;", "and").replace("&", "and")
    slug = re.sub(r'[^a-zA-Z0-9\s-]', '', name.lower())
    slug = re.sub(r'\s+', '-', slug.strip())
    return re.sub(r'-+', '-', slug)


def parse_number(value):
    if not value:
        return None
    value = value.replace(',', '')
    match = re.search(r'(\d+\.?\d*)', value)
    return float(match.group(1)) if match else None


def extract_nutrition(html):
    
    json_blocks = re.findall(r'<script[^>]*type="application/ld\+json"[^>]*>(.*?)</script>', html, re.DOTALL | re.IGNORECASE)
    
    for block in json_blocks:
        try:
            data = json.loads(block)
            
            def find_recipe(obj):
                if isinstance(obj, list):
                    for item in obj:
                        res = find_recipe(item)
                        if res: return res
                elif isinstance(obj, dict):
                    if obj.get('@type') == 'Recipe' or obj.get('@type') == ['Recipe']:
                        return obj
                    if '@graph' in obj:
                        return find_recipe(obj['@graph'])
                return None

            recipe_data = find_recipe(data)
            
            if recipe_data and 'nutrition' in recipe_data:
                nut = recipe_data['nutrition']
                
                total_fat = parse_number(nut.get("fatContent"))
                
                
                calories_from_fat = round(total_fat * 9, 1) if total_fat is not None else None
                
                nutrition_dict = {
                    "calories": parse_number(nut.get("calories")),
                    "calories_from_fat": calories_from_fat,
                    "total_fat_g": total_fat,
                    "saturated_fat_g": parse_number(nut.get("saturatedFatContent")),
                    "cholesterol_mg": parse_number(nut.get("cholesterolContent")),
                    "sodium_mg": parse_number(nut.get("sodiumContent")),
                    "total_carbs_g": parse_number(nut.get("carbohydrateContent")),
                    "dietary_fiber_g": parse_number(nut.get("fiberContent")),
                    "sugars_g": parse_number(nut.get("sugarContent")),
                    "protein_g": parse_number(nut.get("proteinContent"))
                }
                
                return {k: v for k, v in nutrition_dict.items() if v is not None}
                
        except json.JSONDecodeError:
            continue

    return None


def process_recipe(recipe):
    recipe_id = recipe.get("id")
    recipe_name = recipe.get("name")
    
    
    serving_size = recipe.get("serving_size")
    servings = recipe.get("servings")

    if not recipe_id or not recipe_name:
        return None

    slug = name_to_slug(recipe_name)
    url = f"https://www.food.com/recipe/{slug}-{recipe_id}"

    try:
        r = requests.get(url, timeout=10)
        if r.status_code != 200:
            return None

        nutrition = extract_nutrition(r.text)
        if not nutrition:
            return None

        
        final_nutrition = {}
        
        
        if serving_size is not None:
            final_nutrition["serving_size"] = serving_size
        if servings is not None:
            final_nutrition["servings_per_recipe"] = servings
            
        
        final_nutrition.update(nutrition)

        return {
            str(recipe_id): {
                "id": recipe_id,
                "name": recipe_name,
                "nutrition": final_nutrition
            }
        }

    except Exception as e:
        return None


def run_fast_parser():
    with open("resources/recipes_extended.json", "r", encoding="utf-8") as f:
        recipes = json.load(f)

    results = {}
    
    
    if os.path.exists(OUTPUT_FILE):
        with open(OUTPUT_FILE, "r", encoding="utf-8") as f:
            try:
                results = json.load(f)
                print(f"📥 Завантажено {len(results)} вже збережених рецептів.")
            except json.JSONDecodeError:
                pass

    
    start_id = 537642
    start_index = 0
    for i, r in enumerate(recipes):
        if r.get("id") == start_id:
            start_index = i
            break
            
    
    recipes_to_process = recipes[start_index:]
    print(f"🚀 Починаємо парсинг з id {start_id}. Залишилось обробити: {len(recipes_to_process)} рецептів.")

    with ThreadPoolExecutor(max_workers=NUM_WORKERS) as executor:
        futures = [executor.submit(process_recipe, r) for r in recipes_to_process]

        for future in as_completed(futures):
            result = future.result()
            if result:
                results.update(result)
                
                recipe_info = list(result.values())[0]
                print(f"✅ {recipe_info['name']}")
                
                
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)

    print(f"\n🎉 Готово. Всього у файлі тепер збережено {len(results)} рецептів")


if __name__ == "__main__":
    run_fast_parser()