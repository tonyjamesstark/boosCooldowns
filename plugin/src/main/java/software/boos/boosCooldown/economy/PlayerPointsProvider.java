package software.boos.boosCooldown.economy;

import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.model.CommandData;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.entity.Player;

public final class PlayerPointsProvider implements EconomyProvider {

    private final PlayerPoints playerPoints;
    private final MessageConfig messages;
    private final boolean enabled;

    public PlayerPointsProvider(PlayerPoints playerPoints, MessageConfig messages, boolean enabled) {
        this.playerPoints = playerPoints;
        this.messages = messages;
        this.enabled = enabled;
    }

    @Override public String name() { return "PlayerPoints"; }

    @Override public boolean isAvailable() { return enabled && playerPoints != null; }

    @Override
    public boolean appliesTo(CommandData data) {
        Player p = data.player();
        return data.hasPlayerPointsPrice() && !p.hasPermission("booscooldowns.noplayerpoints")
                && !p.hasPermission("booscooldowns.noplayerpoints." + data.originalCommand());
    }

    @Override
    public boolean hasEnough(Player player, CommandData data) {
        return playerPoints != null
                && playerPoints.getAPI().look(player.getUniqueId()) >= data.playerPointsPrice();
    }

    @Override
    public boolean charge(Player player, CommandData data) {
        return playerPoints != null && playerPoints.getAPI().take(player.getUniqueId(), data.playerPointsPrice());
    }

    @Override
    public boolean refund(Player player, CommandData data) {
        return playerPoints != null && playerPoints.getAPI().give(player.getUniqueId(), data.playerPointsPrice());
    }

    @Override
    public String describePayment(Player player, CommandData data) {
        int balance = playerPoints != null ? playerPoints.getAPI().look(player.getUniqueId()) : 0;
        return messages.paidPlayerPointsForCommand()
                .replace("&command&", data.originalCommand())
                .replaceFirst("%s", String.valueOf(data.playerPointsPrice()))
                .replaceFirst("%s", String.valueOf(balance));
    }

    @Override
    public String describeShortage(Player player, CommandData data) {
        return messages.insufficientPlayerPoints()
                .replace("&command&", data.originalCommand())
                .replaceFirst("%s", String.valueOf(data.playerPointsPrice()));
    }
}
