package darkdustry;

import arc.Events;
import arc.struct.Seq;
import arc.util.*;
import arc.util.Timer;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.net.NetConnection;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;
import java.util.Locale;

public class Siege extends Plugin {
    private final Seq<String> cooldowns = new Seq<>();
    int winScore = 1500;
    int foreshadowLimit = 4;

    public void init() {
        netServer.admins.addActionFilter((action) -> {
            if (action.player.team() == Team.green) {
                return !(action.block instanceof Turret && !(action.block == Blocks.wave));
            } else {
                return !(action.block instanceof UnitFactory || action.block instanceof Reconstructor);
	    }
        });

        netServer.admins.addActionFilter((action) -> {
            if (action.type == Administration.ActionType.placeBlock && action.block == Blocks.foreshadow &&
                    Groups.build.count(b -> b.team == action.player.team() && b.block == Blocks.foreshadow) > foreshadowLimit) {
                bundled(action.player, "server.foreshadow-limit", foreshadowLimit);
                return false;
            }
            return true;
        });

        Events.on(GameOverEvent.class, (e) -> {
            cooldowns.clear();
        });

        Events.on(WorldLoadEvent.class, (c) -> {
            winScore = 1500;

            state.rules.unitDamageMultiplier = 2.0F;
            state.rules.unitBuildSpeedMultiplier = 2.0F;
            state.rules.blockDamageMultiplier = 0.6F;
            UnitTypes.poly.weapons.clear();
            Bullets.missileSurge.damage = 10.0F;
            ((ItemTurret)Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).damage = 750;
        });

        Events.on(PlayerJoin.class, event -> {
            // Предлагаем использовать /info
            bundled(event.player, "the-siege.motd");
        });

	Timer.schedule(() -> {
            if (!state.serverPaused) {
	        state.teams.active.each((team) -> {
                    return team.core() != null;
                }, (team) -> content.items().each((item) -> team.core().items.add(item, (int)team.cores.size * 100)));
            }

	    winScore -= state.serverPaused ? 0 : 1;
	    Groups.player.each(p -> Call.infoPopup(p.con(), L10NBundle.format("server.progress", findLocale(p.locale), winScore), 1, Align.bottom, 0, 0, 0, 0));
	    if(winScore<1){
                winScore = 15000;
                sendToChat("server.blue-won");
		Events.fire(new EventType.GameOverEvent(Team.blue));
	    }
	}, 0, 1);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("changeteam", "Change team - once per game.", (args, player) -> {
            if (cooldowns.contains(player.uuid())) {
                bundled(player, "commands.team.cooldown");
                return;
            }
            Team team = player.team() == Team.green ? Team.blue : Team.green;
            player.team(team);
            bundled(player, "commands.team.changed", colorizedTeam(team));
            cooldowns.add(player.uuid());
            player.unit().kill();
        });

        handler.<Player>register("info", "Information about gamemode.", (args, player) -> {
            Call.infoMessage(player.con, L10NBundle.format("commands.info", findLocale(player.locale)));
        });
    }

    // Различные функции, выполняемые в коде.

    private static Locale findLocale(String code) {
        Locale locale = Structs.find(L10NBundle.supportedLocales, l -> l.toString().equals(code) ||
                code.startsWith(l.toString()));
        return locale != null ? locale : L10NBundle.defaultLocale();
    }

    public static void sendToChat(String key, Object... values) {
        Groups.player.each(p -> p.sendMessage(L10NBundle.format(key, findLocale(p.locale), values)));
    }

    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(L10NBundle.format(key, findLocale(player.locale), values));
    }

    public static String colorizedTeam(Team team){
        return Strings.format("[#@]@", team.color, team);
    }
}
