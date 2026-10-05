package software.boos.boosCooldown.service;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.boos.boosCooldown.config.PluginConfig;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Every alias in the production config, parsed by Bukkit's YAML loader, resolves as intended. */
class ProductionAliasesTest {

    @Test
    void everyProductionAliasResolves() throws Exception {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/production-aliases.yml")),
                StandardCharsets.UTF_8));
        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.getAliases()).thenReturn(yaml.getConfigurationSection("commands.aliases").getKeys(false));
        Mockito.when(config.getAlias(Mockito.anyString()))
                .thenAnswer(inv -> yaml.getString("commands.aliases." + inv.getArgument(0)));
        Player player = Mockito.mock(Player.class);
        World world = Mockito.mock(World.class);
        Mockito.when(player.getName()).thenReturn("Steve");
        Mockito.when(world.getName()).thenReturn("world");
        Mockito.when(player.getWorld()).thenReturn(world);
        AliasService service = new AliasService(config);

        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("/ranks", "/guiplus open ranks");
        cases.put("/mutechat", "/togglechat");
        cases.put("/wild", "/tpr");
        cases.put("/rtp", "/tpr");
        cases.put("/trails", "/pp");
        cases.put("/shop gui", "/arm gui");
        cases.put("/shop settp shop1 here", "/arm settplocation shop1 here");
        cases.put("/pshop Bob", "/arm tp Bob");
        cases.put("/stp Bob", "/arm tp Bob");
        cases.put("/unignore Bob", "/unignoreplayer Bob");
        cases.put("/ignore Bob", "/ignoreplayer Bob");
        cases.put("/ignorelist", "/ignoredplayerlist");
        cases.put("/colours", "/colors");
        cases.put("/sign", "/esign");
        cases.put("/pc", "/party chat");
        cases.put("/stats Bob", "/plan ingame Bob");
        cases.put("/scut", "/stonecutter");
        cases.put("/smith", "/smithingtable");
        cases.put("/kits", "/kit");
        cases.put("/es", "/esign");
        cases.put("/es set 1 hello there", "/esign set 1 hello there");
        cases.put("/warp traderoom", "/warp trademarket");
        cases.put("/layouts 2", "/einfo layouts 2");
        cases.put("/layouts", "/einfo layouts");
        cases.put("/tell Bob hi there you", "/msg Bob hi there you");
        cases.put("/whisper Bob hi", "/msg Bob hi");
        cases.put("/wb", "/workbench");
        cases.put("/warp", "/gui open warps");
        cases.put("/warps", "/gui open warps");
        cases.put("/buy", "/einfo buy");
        cases.put("/bro server restart soon", "/broadcast server restart soon");
        cases.put("/info", "/einfo info");
        cases.put("/info rules", "/einfo rules");
        cases.put("/death", "/suicide");
        cases.put("/crates", "/warp crates");
        cases.put("/wither", "/warp witherskeleton");
        cases.put("/newcommand", "/originalcommand");
        cases.put("/bs token", "/warp TradeMarket");
        cases.put("/shops", "/warp dark_market");
        cases.put("/maze", "/warp maze");
        cases.put("/events", "/cmi ctext events");
        cases.put("/bed", "/home bed");
        cases.put("/tiles", "//count 23,54,61,68,117,138,144,146,154,158,176,177,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234");
        cases.put("/shophelp", "/warp shophelp");
        cases.put("/trademarket", "/warp trademarket");
        cases.put("/phead_boos Notch", "/minecraft:give Steve minecraft:player_head[profile={name:\"Notch\"}]");
        cases.put("/phead_divine Notch", "/minecraft:give Steve player_head[enchantments={blast_protection:4,fire_protection:4,projectile_protection:4,protection:5,aqua_affinity:1,respiration:3},attribute_modifiers=[{type:armor,id:armor,amount:5,operation:add_value,slot:head},{type:armor_toughness,id:armor_toughness,amount:4,operation:add_value,slot:head},{type:attack_damage,id:attack_damage,amount:5,operation:add_value,slot:head}],profile={name:\"Notch\"},lore=[[\"\",{\"color\":\"white\",\"text\":\"CE Glowing I*\",\"italic\":false}]]]");
        cases.put("/cll", "/centerlorelines");
        cases.put("/run-script foo bar", "/ce call foo bar");
        cases.put("/pvlist", "/papi parse Steve %premiumvanish_vanishedplayers%");
        cases.put("/bomb Bob", "/execute as Bob at Bob run function swanfarms:bomb");
        // Commands that share a prefix with an alias must pass through untouched.
        for (String passthrough : new String[] {"/warp spawn", "/warpinfo", "/shopkeeper", "/esign", "/information",
                "/tellraw @a hi", "/ignoreplayer Bob", "/bombs", "/stats", "/wildlife", "/kits extra", "/pcs"}) {
            cases.put(passthrough, null);
        }

        assertAll(cases.entrySet().stream().map(e -> () -> {
            String actual = service.resolve(player, e.getKey());
            if (e.getValue() == null) assertNull(actual, e.getKey());
            else assertEquals(e.getValue(), actual, e.getKey());
        }));
    }
}
