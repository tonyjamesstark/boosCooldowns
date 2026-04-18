package software.boos.boosCooldown.economy;

import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.model.CommandData;
import org.bukkit.entity.Player;

public final class XpProvider implements EconomyProvider {

    private final MessageConfig messages;
    private final boolean enabled;

    public XpProvider(MessageConfig messages, boolean enabled) {
        this.messages = messages;
        this.enabled = enabled;
    }

    @Override public String name() { return "XP"; }

    @Override public boolean isAvailable() { return enabled; }

    @Override
    public boolean appliesTo(CommandData data) {
        Player p = data.player();
        return (data.hasXpPrice() || data.hasXpRequirement())
                && !p.hasPermission("booscooldowns.noxpcost")
                && !p.hasPermission("booscooldowns.noxpcost." + data.originalCommand());
    }

    @Override
    public boolean hasEnough(Player player, CommandData data) {
        if (data.hasXpRequirement() && player.getLevel() < data.xpRequirement()) {
            return false;
        }
        return !data.hasXpPrice() || player.getLevel() >= data.xpPrice();
    }

    @Override
    public boolean charge(Player player, CommandData data) {
        if (data.hasXpPrice()) {
            player.setLevel(player.getLevel() - data.xpPrice());
        }
        return true;
    }

    @Override
    public boolean refund(Player player, CommandData data) {
        if (!data.hasXpPrice()) return true;
        player.setLevel(player.getLevel() + data.xpPrice());
        return true;
    }

    @Override
    public String describePayment(Player player, CommandData data) {
        return messages.paidXpForCommand()
                .replace("&command&", data.originalCommand())
                .replaceFirst("%s", String.valueOf(data.xpPrice()));
    }

    @Override
    public String describeShortage(Player player, CommandData data) {
        if (data.hasXpRequirement() && player.getLevel() < data.xpRequirement()) {
            return messages.insufficientXpRequirement()
                    .replace("&command&", data.originalCommand())
                    .replaceFirst("%s", String.valueOf(data.xpRequirement()));
        }
        return messages.insufficientXp()
                .replace("&command&", data.originalCommand())
                .replaceFirst("%s", String.valueOf(data.xpPrice()));
    }
}
