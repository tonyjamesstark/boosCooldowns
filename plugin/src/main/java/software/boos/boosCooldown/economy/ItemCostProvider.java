package software.boos.boosCooldown.economy;

import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.model.ItemCost;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

public final class ItemCostProvider implements EconomyProvider {

    private final MessageConfig messages;
    private final boolean enabled;

    public ItemCostProvider(MessageConfig messages, boolean enabled) {
        this.messages = messages;
        this.enabled = enabled;
    }

    @Override public String name() { return "ItemCost"; }

    @Override public boolean isAvailable() { return enabled; }

    @Override
    public boolean appliesTo(CommandData data) {
        Player p = data.player();
        return data.hasItemCost()
                && !p.hasPermission("booscooldowns.noitemcost")
                && !p.hasPermission("booscooldowns.noitemcost." + data.originalCommand());
    }

    @Override
    public boolean hasEnough(Player player, CommandData data) {
        return countMatching(player, data.itemCost()) >= data.itemCost().count();
    }

    @Override
    public boolean charge(Player player, CommandData data) {
        int remaining = data.itemCost().count();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;
            if (!matches(stack, data.itemCost())) continue;
            int amount = stack.getAmount();
            if (amount <= remaining) {
                remaining -= amount;
                contents[i] = null;
            } else {
                stack.setAmount(amount - remaining);
                remaining = 0;
            }
        }
        player.getInventory().setContents(contents);
        player.updateInventory();
        return remaining == 0;
    }

    @Override
    public boolean refund(Player player, CommandData data) {
        Material material = Material.matchMaterial(data.itemCost().material());
        if (material == null) return false;
        ItemStack refund = new ItemStack(material, data.itemCost().count());
        player.getInventory().addItem(refund).forEach((index, leftover) ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        return true;
    }

    @Override
    public String describePayment(Player player, CommandData data) {
        return messages.paidItemsForCommand()
                .replace("&command&", data.originalCommand())
                .replaceFirst("%s", data.itemCost().count() + " " + data.itemCost().material());
    }

    @Override
    public String describeShortage(Player player, CommandData data) {
        return messages.insufficientItems()
                .replace("&command&", data.originalCommand())
                + " " + data.itemCost().count() + " " + data.itemCost().material();
    }

    private int countMatching(Player player, ItemCost cost) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && matches(stack, cost)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private boolean matches(ItemStack stack, ItemCost cost) {
        Material material = Material.matchMaterial(cost.material());
        if (material == null || stack.getType() != material) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!cost.displayName().isEmpty()) {
            if (meta == null || !meta.hasDisplayName() || !cost.displayName().equals(meta.getDisplayName())) {
                return false;
            }
        }
        if (!cost.lore().isEmpty()) {
            if (meta == null || !meta.hasLore() || !new ArrayList<>(cost.lore()).equals(meta.getLore())) {
                return false;
            }
        }
        if (!cost.enchants().isEmpty()) {
            if (meta == null) return false;
            for (String enchantSpec : cost.enchants()) {
                String[] parts = enchantSpec.split(":");
                if (parts.length != 2) continue;
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                if (enchantment == null) continue;
                int level;
                try {
                    level = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (meta.getEnchantLevel(enchantment) < level) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<ItemStack> buildCostPreview(ItemCost cost) {
        List<ItemStack> list = new ArrayList<>();
        Material material = Material.matchMaterial(cost.material());
        if (material != null) {
            list.add(new ItemStack(material, cost.count()));
        }
        return list;
    }
}
