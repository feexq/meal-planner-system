import json
import google.generativeai as genai


API_KEY = "YOUR_API_KEY_HERE"

genai.configure(api_key=API_KEY)

model = genai.GenerativeModel("gemini-3.1-flash-lite-preview")


def extract_recipe_core(recipe):
    return {
        "id": recipe["id"],
        "name": recipe["name"],
        "ingredients": recipe.get("ingredients"),
        "meal_type": recipe.get("meal_type"),
        "cook_time": recipe.get("cook_time"),
        "cook_complexity": recipe.get("cook_complexity"),
        "cook_budget": recipe.get("cook_budget")
    }


def extract_nutrition_core(nutrition):
    return {
        "calories": nutrition["nutrition"]["calories"],
        "protein": nutrition["nutrition"]["protein_g"],
        "carbs": nutrition["nutrition"]["total_carbs_g"],
        "fat": nutrition["nutrition"]["total_fat_g"],
        "sodium": nutrition["nutrition"]["sodium_mg"]
    }


def build_candidates(slot, recipes, nutritions, top_k=3):

    result = []

    for candidate in slot["candidates"][:top_k]:

        rid = candidate["recipeId"]

        recipe = recipes[str(rid)]
        nutrition = nutritions[str(rid)]

        result.append({
            "id": rid,
            "name": candidate["recipeName"],
            "score": candidate["score"],
            "ingredients": recipe["ingredients"],
            "nutrition": extract_nutrition_core(nutrition)
        })

    return result


def build_meal_plan_context(meal_plan, recipes, nutritions):

    days = []

    for d in meal_plan["days"]:

        day_data = {
            "day": d["day"],
            "breakfast_options": [],
            "lunch_options": [],
            "dinner_options": []
        }

        for slot in d["slots"]:

            meal = slot["mealType"]

            options = build_candidates(slot, recipes, nutritions)

            if meal == "breakfast":
                day_data["breakfast_options"] = options

            if meal == "lunch":
                day_data["lunch_options"] = options

            if meal == "dinner":
                day_data["dinner_options"] = options

        days.append(day_data)

    return days


def build_prompt(user_profile, nutrition_targets, meal_context):

    return f"""
You are a professional nutritionist AI.

Your task:
Select the BEST recipe for each meal for 7 days.

Consider carefully:
- user allergies
- health conditions
- calorie targets
- macro balance
- sodium intake
- ingredient dislikes
- cooking complexity
- dietary type

User profile:
{json.dumps(user_profile, indent=2)}

Nutrition targets:
{json.dumps(nutrition_targets, indent=2)}

Meal candidates:
{json.dumps(meal_context, indent=2)}

Select ONE recipe for each meal.

Return STRICT JSON ONLY.

JSON format:

{ 
  "days":[
    { 
      "day":1,
      "breakfast":{ "recipeId":123,"name":"recipe name"} ,
      "lunch":{ "recipeId":123,"name":"recipe name"} ,
      "dinner":{ "recipeId":123,"name":"recipe name"} ,
      "notes":"3-4 sentences explaining why this day of meals fits this specific user (health conditions, calorie targets, dietary preferences, etc.)"
    } 
  ]
} 

Important:
- Notes MUST be 3-4 sentences.
- Explain WHY the meals are suitable for THIS user.
- Mention health conditions or goals if relevant.
- Do NOT include any text outside JSON.
"""


def generate_llm_meal_plan(meal_plan, recipes, nutritions, user_profile):

    meal_context = build_meal_plan_context(meal_plan, recipes, nutritions)

    prompt = build_prompt(
        user_profile,
        meal_plan["days"][0]["estimatedDailyMacros"],
        meal_context
    )

    response = model.generate_content(prompt)

    text = response.text.strip()

    return json.loads(text)


if __name__ == "__main__":

    with open("meal_plan_user-001_1.json", encoding="utf-8") as f:
        meal_plan = json.load(f)

    with open("recipes_extended_updated.json", encoding="utf-8") as f:
        recipes = json.load(f)

    recipes_list = recipes
    recipes = {str(r["id"]): r for r in recipes_list}

    with open("nutrition_data_formatted.json", encoding="utf-8") as f:
        nutritions = json.load(f)

    with open("survey.json", encoding="utf-8") as f:
        user_profile = json.load(f)

    result = generate_llm_meal_plan(
        meal_plan,
        recipes,
        nutritions,
        user_profile
    )

    with open("final_meal_plan_llm.json", "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    print("Meal plan saved to final_meal_plan_llm.json")

    print(json.dumps(result, indent=2))