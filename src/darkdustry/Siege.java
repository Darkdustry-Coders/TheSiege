package darkdustry;

import arc.Events;
import arc.struct.ObjectMap;
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
import java.util.Iterator;
import java.util.HashSet;
import java.util.Locale;

public class Siege extends Plugin {
    private final HashSet<String> cooldowns = new HashSet<>();
    int winScore = 1500;

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
                    Groups.build.count(b -> b.team == action.player.team() && b.block == action.block) > 4) {
                bundled(action.player, "server.foreshadow-limit");
                return false;
            }
            return true;
        });

        Events.on(GameOverEvent.class, (e) -> {
            cooldowns.clear();
        });

        Events.on(WorldLoadEvent.class, (c) -> {
            winScore = 1500;

            UnitTypes.poly.health = 4000;
            UnitTypes.poly.weapons.clear();
            for(int i = 0; i < 11; i++) {
                UnitTypes.poly.spawn(Team.blue, (float)world.width() * 4, (float)world.height() * 4);
            }

            // Нерф знамения и циклона
            state.rules.unitDamageMultiplier = 1.5F;
            Bullets.missileSurge.damage = 12.0F;
            ((ItemTurret)Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).damage = 800;
        });

        Events.on(PlayerJoin.class, event -> {
            // Предлагаем использовать /info
            bundled(event.player, "the-siege.motd");
        });

	Timer.schedule(() -> {
            if (!state.serverPaused) {
	        state.teams.active.each((team) -> {
                    return team.core() != null;
                }, (team) -> content.items().each((item) -> team.core().items.add(item, 50)));
            }

	    winScore -= state.serverPaused ? 0 : 1;
	    Groups.player.each(p -> Call.infoPopup(p.con(), L10NBundle.format("server.progress", findLocale(p.locale), winScore), 1, Align.bottom, 0, 0, 0, 0));
	    if(winScore<1){
                winScore = 15000;
		Events.fire(new EventType.GameOverEvent(Team.blue));
		sendToChat("server.blue-won");
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
            if (player.team() == Team.blue) {
                player.team(Team.green);
                bundled(player, "commands.team.changed", colorizedTeam(Team.green));
                cooldowns.add(player.uuid());
                player.unit().kill();
            } else if (player.team() == Team.green) {
                player.team(Team.blue);
                bundled(player, "commands.team.changed", colorizedTeam(Team.blue));
                cooldowns.add(player.uuid());
                player.unit().kill();
            } else {
                bundled(player, "commands.team.error");
            }
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
