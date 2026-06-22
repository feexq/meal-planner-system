from fastapi import APIRouter, HTTPException

from models.schemas import (
    FinalizeRequest, FinalizeResponse,
    FinalizedDay, FinalizedSlot, MealItem,
)
from services.finalizer import finalize_plan

router = APIRouter(tags=["finalize"])


def _to_meal_item(m: dict | None) -> MealItem | None:
    if not m:
        return None
    recipe_id = m.get("recipeId") or m.get("id") or m.get("sideId") or m.get("mainId")
    if recipe_id is None:
        raise ValueError(f"Missing recipeId in: {m}")
    est_cals = (
            m.get("estimatedCalories") or m.get("calories")
            or m.get("scaledCalories") or m.get("cal") or 0
    )
    return MealItem(
        recipeId=recipe_id,
        name=m.get("name") or m.get("n", "Unknown"),
        servings=m.get("servings") or m.get("srv", 1.0),
        estimatedCalories=est_cals,
        portionNote=m.get("portionNote", ""),
        proteinG=m.get("proteinG") or m.get("p", 0.0),
        carbsG=m.get("carbsG")   or m.get("c", 0.0),
        fatG=m.get("fatG")       or m.get("f", 0.0),
    )


import uuid
from threading import Thread

tasks = {}

def _process_finalize(request: FinalizeRequest) -> FinalizeResponse:
    try:
        parsed = finalize_plan(request.model_dump())
    except Exception as exc:
        raise HTTPException(status_code=503, detail=str(exc))

    finalized_days = []
    for day_data in parsed.get("days", []):
        slots = []
        for slot_data in day_data.get("slots", []):
            main = _to_meal_item(slot_data.get("main"))
            side = _to_meal_item(slot_data.get("side"))

            calc_cals  = (main.estimatedCalories if main else 0) + (side.estimatedCalories if side else 0)
            calc_prot  = (main.proteinG if main else 0)          + (side.proteinG if side else 0)
            calc_carbs = (main.carbsG if main else 0)            + (side.carbsG if side else 0)
            calc_fat   = (main.fatG if main else 0)              + (side.fatG if side else 0)

            slots.append(FinalizedSlot(
                mealType=slot_data["mealType"],
                calorieBudget=slot_data.get("calorieBudget", 0),
                main=main,
                side=side,
                slotTotalCalories=round(calc_cals, 1),
                slotProteinG=round(calc_prot, 1),
                slotCarbsG=round(calc_carbs, 1),
                slotFatG=round(calc_fat, 1),
            ))

        finalized_days.append(FinalizedDay(
            day=day_data.get("day", day_data.get("d", 0)),
            dailyCalorieTarget=day_data.get("dailyCalorieTarget", request.dailyCalorieTarget),
            slots=slots,
            dayTotalCalories=day_data.get("dayTotalCalories", 0),
            dailyProteinG=day_data.get("dailyProteinG", 0.0),
            dailyCarbsG=day_data.get("dailyCarbsG", 0.0),
            dailyFatG=day_data.get("dailyFatG", 0.0),
            notes=day_data.get("notes", ""),
        ))

    return FinalizeResponse(userId=request.userId, days=finalized_days)


@router.post("/finalize", response_model=FinalizeResponse)
def finalize_meal_plan(request: FinalizeRequest):
    return _process_finalize(request)


def background_finalize(task_id: str, request: FinalizeRequest):
    try:
        result = _process_finalize(request)
        tasks[task_id] = {"status": "COMPLETED", "result": result.model_dump()}
    except Exception as e:
        tasks[task_id] = {"status": "ERROR", "error": str(e)}


@router.post("/finalize/async")
def finalize_meal_plan_async(request: FinalizeRequest):
    task_id = str(uuid.uuid4())
    tasks[task_id] = {"status": "PROCESSING"}
    thread = Thread(target=background_finalize, args=(task_id, request))
    thread.start()
    return {"jobId": task_id}


@router.get("/finalize/status/{task_id}")
def get_finalize_status(task_id: str):
    if task_id not in tasks:
        raise HTTPException(status_code=404, detail="Task not found")
    data = tasks[task_id]
    if data["status"] in ("COMPLETED", "ERROR"):
        return tasks.pop(task_id)
    return data