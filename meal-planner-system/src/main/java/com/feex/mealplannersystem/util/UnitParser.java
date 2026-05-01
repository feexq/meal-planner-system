package com.feex.mealplannersystem.util;

import com.feex.mealplannersystem.util.record.ParsedUnit;
import org.springframework.stereotype.Component;

@Component
public class UnitParser {

    private static final String DEFAULT_UNIT = "шт";
    private static final double DEFAULT_AMOUNT = 1.0;

    public ParsedUnit parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedUnit(DEFAULT_AMOUNT, DEFAULT_UNIT);
        }

        String trimmed = raw.trim();
        String numberPart = trimmed.replaceAll("[^0-9.,]", "").replace(",", ".");
        String textPart = trimmed.replaceAll("[0-9.,\\s]", "").toLowerCase();

        try {
            double amount = numberPart.isEmpty()
                    ? DEFAULT_AMOUNT
                    : Double.parseDouble(numberPart);
            String unit = textPart.isEmpty() ? DEFAULT_UNIT : textPart;
            return new ParsedUnit(amount, unit);
        } catch (NumberFormatException e) {
            return new ParsedUnit(DEFAULT_AMOUNT, DEFAULT_UNIT);
        }
    }
}
