from __future__ import annotations

from pydantic import BaseModel
from typing import Any


# ─── Normalize ────────────────────────────────────────────────────────────────

class NormalizeRequest(BaseModel):
    raw_name: str

class NormalizeResponse(BaseModel):
    raw_name: str
    normalized_name: str


# ─── Classify ─────────────────────────────────────────────────────────────────

class ClassifyRequest(BaseModel):
    ingredients: list[str]

class ClassifyResponse(BaseModel):
    results: dict
    failed: list[str]


# ─── Food parse ───────────────────────────────────────────────────────────────

class ParseFoodRequest(BaseModel):
    text: str
    language: str = "auto"

class FoodItem(BaseModel):
    name: str
    original: str
    quantity_description: str
    calories: float
    protein_g: float
    carbs_g: float
    fat_g: float
    confidence: str
    from_cache: bool = False

class ParseFoodResponse(BaseModel):
    items: list[FoodItem]
    total_calories: float
    total_protein_g: float
    total_carbs_g: float
    total_fat_g: float
    parse_note: str | None = None


# ─── Finalize ─────────────────────────────────────────────────────────────────

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
    carbsG: float   = 0.0
    fatG: float     = 0.0
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

class FinalizedDay(BaseModel):
    day: int
    dailyCalorieTarget: int
    slots: list[FinalizedSlot]
    dayTotalCalories: float
    dailyProteinG: float = 0.0
    dailyCarbsG: float   = 0.0
    dailyFatG: float     = 0.0
    notes: str

class FinalizeResponse(BaseModel):
    userId: str
    days: list[FinalizedDay]


# ─── Swap slot ────────────────────────────────────────────────────────────────

class SwapSlotMainRequest(BaseModel):
    slotId: int
    mealType: str
    currentRecipeId: int
    calorieBudget: float
    userProfile: UserProfileRequest
    candidates: list[CandidateItem]
    useLlm: bool = False

class SwapSlotSideRequest(BaseModel):
    slotId: int
    mealType: str
    calorieBudget: float
    mainRecipe: dict
    additionalPool: list[AdditionalRecipeItem]
    userProfile: UserProfileRequest

class SwapSlotSideResponse(BaseModel):
    slotId: int
    chosen: dict | None
    message: str

class SwapSlotMainResponse(BaseModel):
    slotId: int
    chosen: dict | None
    message: str