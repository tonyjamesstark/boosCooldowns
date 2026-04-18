package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.HelpCategory;
import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.ConfirmationService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CancelSubCommand implements SubCommand {

    private final ConfirmationService confirmations;

    public CancelSubCommand(ConfirmationService confirmations) {
        this.confirmations = confirmations;
    }

    @Override public String name() { return "cancel"; }
    @Override public String permission() { return ""; }
    @Override public HelpCategory category() { return HelpCategory.INTERNAL; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            BoosChat.sendMessage(sender, "&cOnly players can cancel commands");
            return true;
        }
        if (args.length < 1) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd cancel <token>");
            return true;
        }
        confirmations.cancel(player, args[0]);
        return true;
    }
}
