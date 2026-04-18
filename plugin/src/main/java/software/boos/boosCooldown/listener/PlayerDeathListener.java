package software.boos.boosCooldown.listener;

import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.service.CommandDataFactory;
import software.boos.boosCooldown.service.CooldownService;
import software.boos.boosCooldown.service.LimitService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerDeathListener implements Listener {

    private final PluginConfig config;
    private final CommandDataFactory factory;
    private final CooldownService cooldowns;
    private final LimitService limits;

    public PlayerDeathListener(PluginConfig config, CommandDataFactory factory,
                               CooldownService cooldowns, LimitService limits) {
        this.config = config;
        this.factory = factory;
        this.cooldowns = cooldowns;
        this.limits = limits;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (config.isClearCooldownsOnDeath()
                && player.hasPermission("booscooldowns.clear.cooldowns.death")) {
            cooldowns.resetCooldowns(player);
        }
        if (config.isClearUsesOnDeath()
                && player.hasPermission("booscooldowns.clear.uses.death")) {
            limits.resetUsesForPlayer(player);
        }
        if (config.isStartCooldownsOnDeath()
                && !player.hasPermission("booscooldowns.start.cooldowns.death.exception")) {
            for (String key : config.getCommandsForPlayer(player)) {
                CommandData data = factory.build(player, key, key);
                if (data.hasCooldown()) {
                    cooldowns.setCooldown(player, data);
                }
            }
        }
    }
}
