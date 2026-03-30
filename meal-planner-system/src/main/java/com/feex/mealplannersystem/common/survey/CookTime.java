package com.feex.mealplannersystem.common.survey;

public enum CookTime {
    MIN_15, MIN_30, MIN_60, HOURS_4, DAYS_1_PLUS,
    FIFTEEN_MIN,
    THIRTY_MIN,
    SIXTY_MIN,
    FOUR_HOURS,
    ONE_PLUS_DAYS;

    public static CookTime fromString(String value) {
        return switch (value) {
            case "15 min"   -> MIN_15;
            case "30 min"   -> MIN_30;
            case "60 min"   -> MIN_60;
            case "4 hours"  -> HOURS_4;
            case "1+ days"  -> DAYS_1_PLUS;
            default         -> MIN_60;
        };
    }
}
