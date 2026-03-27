package model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ClassifiedCondition {
    @SerializedName("hard_forbidden")
    private List<String> hardForbidden;

    @SerializedName("soft_forbidden")
    private List<String> softForbidden;

    public List<String> getHardForbidden() {
        return hardForbidden;
    }

    public List<String> getSoftForbidden() {
        return softForbidden;
    }
}
