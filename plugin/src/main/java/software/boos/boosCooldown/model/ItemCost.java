package software.boos.boosCooldown.model;

import java.util.List;

public record ItemCost(
        String material,
        int count,
        String displayName,
        List<String> lore,
        List<String> enchants
) {
    public ItemCost {
        lore = lore == null ? List.of() : List.copyOf(lore);
        enchants = enchants == null ? List.of() : List.copyOf(enchants);
    }
}
