package model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class Ingredient {
    private String name;
    private List<String> contraindications;

    @SerializedName("not_suitable_for_diets")
    private List<String> notSuitableForDiets;

    @SerializedName("consumption_caveats")
    private Map<String, String> consumptionCaveats;

    public String getName() {
        return name;
    }

    public List<String> getContraindications() {
        return contraindications;
    }

    public List<String> getNotSuitableForDiets() {
        return notSuitableForDiets;
    }

    public Map<String, String> getConsumptionCaveats() {
        return consumptionCaveats;
    }
}
