package model;

import java.util.List;

public class DietaryReference {
    private List<ReferenceItem> contraindications;
    private List<ReferenceItem> diets;

    public List<ReferenceItem> getContraindications() {
        return contraindications;
    }

    public List<ReferenceItem> getDiets() {
        return diets;
    }
}
