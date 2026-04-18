package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.HelpCategory;
import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.ConfirmationService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ConfirmationsSubCommand implements SubCommand {

    private final ConfirmationService confirmations;

    public ConfirmationsSubCommand(ConfirmationService confirmations) {
        this.confirmations = confirmations;
    }

    @Override public String name() { return "confirmations"; }
    @Override public String permission() { return ""; }
    @Override public HelpCategory category() { return HelpCategory.PLAYER; }
    @Override public String description() { return "Toggle the confirmation dialog on/off"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            BoosChat.sendMessage(sender, "&cOnly players can toggle confirmations");
            return true;
        }
        confirmations.toggle(player);
        return true;
    }
}
