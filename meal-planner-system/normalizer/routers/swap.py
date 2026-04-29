from fastapi import APIRouter, HTTPException

from models.schemas import (
    SwapSlotMainRequest, SwapSlotMainResponse,
    SwapSlotSideRequest, SwapSlotSideResponse,
)
from services.meal_swap import swap_main_via_llm, swap_side_via_llm

router = APIRouter(prefix="/swap-slot", tags=["swap"])


@router.post("/side", response_model=SwapSlotSideResponse)
def swap_side(req: SwapSlotSideRequest):
    pool = [r.model_dump() for r in req.additionalPool]

    if not pool:
        raise HTTPException(status_code=400, detail="additionalPool is empty")

    chosen = swap_side_via_llm(
        slot_info      = {"mealType": req.mealType},
        main_recipe    = req.mainRecipe,
        additional_pool= pool,
        user_profile   = req.userProfile.model_dump(),
        calorie_budget = req.calorieBudget,
    )

    if chosen is None:
        return SwapSlotSideResponse(
            slotId=req.slotId,
            chosen=None,
            message=f"No suitable side found within {req.calorieBudget:.0f} kcal budget",
        )

    return SwapSlotSideResponse(
        slotId=req.slotId,
        chosen=chosen,
        message="Side replaced successfully",
    )


@router.post("/main", response_model=SwapSlotMainResponse)
def swap_main(req: SwapSlotMainRequest):
    candidates = [c.model_dump() for c in req.candidates]
    filtered   = [c for c in candidates if c["recipeId"] != req.currentRecipeId]

    if not filtered:
        raise HTTPException(
            status_code=400,
            detail="No alternative candidates available for this slot",
        )

    if req.useLlm:
        chosen = swap_main_via_llm(
            slot_info         = {"mealType": req.mealType},
            current_recipe_id = req.currentRecipeId,
            candidates        = filtered,
            user_profile      = req.userProfile.model_dump(),
            calorie_budget    = req.calorieBudget,
        )
    else:
        by_score = sorted(filtered, key=lambda c: c["score"], reverse=True)
        chosen   = None
        for c in by_score:
            scaled = c["scaledCalories"]
            if req.calorieBudget * 0.80 <= scaled <= req.calorieBudget * 1.20:
                chosen = c
                break
        if chosen is None:
            chosen = by_score[0]

    if chosen is None:
        return SwapSlotMainResponse(
            slotId=req.slotId,
            chosen=None,
            message="No suitable main recipe found",
        )

    return SwapSlotMainResponse(
        slotId=req.slotId,
        chosen=chosen,
        message="Main recipe replaced successfully",
    )