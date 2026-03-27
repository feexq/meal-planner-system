package model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Tag {
    private String name;
    private List<String> contraindications;

    @SerializedName("not_suitable_for_diets")
    private List<String> notSuitableForDiets;

    public String getName() {
        return name;
    }

    public List<String> getContraindications() {
        return contraindications;
    }

    public List<String> getNotSuitableForDiets() {
        return notSuitableForDiets;
    }
}
