package software.boos.boosCooldown.economy;

import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.model.CommandData;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

public final class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;
    private final MessageConfig messages;
    private final boolean enabled;

    public VaultEconomyProvider(Economy economy, MessageConfig messages, boolean enabled) {
        this.economy = economy;
        this.messages = messages;
        this.enabled = enabled;
    }

    @Override public String name() { return "Vault"; }

    @Override public boolean isAvailable() { return enabled && economy != null; }

    @Override
    public boolean appliesTo(CommandData commandData) {
        return commandData.hasMoneyPrice() && !commandData.player().hasPermission("booscooldowns.noprice")
                && !commandData.player().hasPermission("booscooldowns.noprice." + commandData.originalCommand());
    }

    @Override
    public boolean hasEnough(Player player, CommandData commandData) {
        return economy != null && economy.has(player, commandData.moneyPrice());
    }

    @Override
    public boolean charge(Player player, CommandData commandData) {
        if (economy == null) return false;
        EconomyResponse response = economy.withdrawPlayer(player, commandData.moneyPrice());
        return response.transactionSuccess();
    }

    @Override
    public String describePayment(Player player, CommandData commandData) {
        double balance = economy != null ? economy.getBalance(player) : 0.0;
        return messages.paidForCommand()
                .replace("&command&", commandData.originalCommand())
                .replaceFirst("%s", String.valueOf(commandData.moneyPrice()))
                .replaceFirst("%s", String.valueOf(balance));
    }

    @Override
    public boolean refund(Player player, CommandData data) {
        return economy != null && economy.depositPlayer(player, data.moneyPrice()).transactionSuccess();
    }

    @Override
    public String describeShortage(Player player, CommandData commandData) {
        double balance = economy != null ? economy.getBalance(player) : 0.0;
        return messages.insufficientFunds()
                .replace("&command&", commandData.originalCommand())
                .replaceFirst("%s", String.valueOf(commandData.moneyPrice()))
                .replaceFirst("%s", String.valueOf(balance));
    }
}
