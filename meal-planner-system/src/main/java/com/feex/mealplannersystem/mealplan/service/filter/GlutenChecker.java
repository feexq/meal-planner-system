package com.feex.mealplannersystem.mealplan.service.filter;

import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@NoArgsConstructor
public class GlutenChecker {
    private static final Set<String> GLUTEN_SAFE = Set.of(
            "corn tortilla","corn flour","rice flour","rice bread","gluten-free bread",
            "gluten-free flour","gluten-free pasta","gluten-free noodle","gluten-free cracker",
            "gluten-free soy sauce","tamari","rice noodle","rice paper","cornbread",
            "corn starch","arrowroot","white miso","yellow miso","red miso","rice miso",
            "shiro miso","sweet miso");

    private static final List<String> GLUTEN_KW = List.of(
            "wheat","flour","barley","rye","spelt","kamut","triticale",
            "bread","toast","croissant","baguette","pita","pretzel",
            "pasta","spaghetti","macaroni","noodle","couscous","orzo",
            "fettuccine","linguine","penne","lasagna","ravioli","gnocchi",
            "semolina","bulgur","pastry","crouton","cracker","breadcrumb",
            "panko","tortilla","filo","phyllo","dumpling","wonton",
            "soy sauce","teriyaki","mugi miso","barley miso",
            "seitan","wheat germ","wheat bran","malt","malt vinegar");

    public static boolean containsGlutenIngredient(RecipeModel recipe) {
        for (String ing : recipe.getParsedIngredients()) {
            String il = ing.toLowerCase().trim();
            if (GLUTEN_SAFE.stream().anyMatch(il::contains)) continue;
            if (GLUTEN_KW.stream().anyMatch(il::contains)) return true;
        }
        return false;
    }
}
