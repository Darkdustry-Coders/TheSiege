package darkdustry;

import arc.Events;
import arc.struct.ObjectMap;
import arc.util.*;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.NetConnection;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.units.*;
import mindustry.content.*;

import java.util.Iterator;
import java.util.HashSet;
import java.util.Locale;

public class Siege extends Plugin {
    private final HashSet<String> cooldowns = new HashSet<>();
    int winScore = 1500;
    int updateInterval = 1;
    int topLength = 5;

    public void init() {
        Vars.netServer.admins.addActionFilter((action) -> {
            if (action.player.team() == Team.green) {
                return !(action.block instanceof Turret);
            } else {
                return !(action.block instanceof UnitFactory);
	    }
        });

        Events.on(GameOverEvent.class, (e) -> {
            cooldowns.clear();
        });
        Events.on(WorldLoadEvent.class, (c) -> {
            winScore = 1500;
            UnitTypes.poly.weapons.clear();
            UnitTypes.mega.weapons.clear();
            UnitTypes.quad.weapons.clear();

            for(int i = 0; i < 11; i++) {
                UnitTypes.poly.spawn(Team.blue, (float)Vars.world.width() / 2, (float)Vars.world.height() / 2);
            }

            ((ItemTurret)Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).damage = 800;

            UnitTypes.eclipse.health = 22000.0F;
            UnitTypes.corvus.health = 22000.0F;

            Vars.state.rules.unitDamageMultiplier = 1.5F;
            Bullets.missileSurge.damage = 12.0F;

            Iterator var1 = Groups.player.iterator();
            while(var1.hasNext()) {
                Player player = (Player)var1.next();
                if (player.team() == Team.blue) {
                    player.team(Team.green);
                } else {
                    player.team(Team.blue);
                }
            }
        });

        Vars.netServer.admins.addActionFilter(action -> {
            if (action.type == Administration.ActionType.placeBlock && action.block == Blocks.foreshadow &&
                    Groups.build.count(b -> b.team == action.player.team() && b.block == action.block) > 8) {
                bundled(action.player, "server.foreshadow-limit");
                return false;
            }
            if (action.type == Administration.ActionType.depositItem) {
                if (action.item == Items.blastCompound || action.item == Items.pyratite) {
                    return false;
                }
            }
            return true;
        });

	Timer.schedule(() -> {
            if (Vars.state.serverPaused == false) {
	        Vars.state.teams.active.each((team) -> {
                    return team.core() != null;
                }, (team) -> {
                    Vars.content.items().each((item) -> {
                        team.core().items.add(item, 50);
		    });
                });
            }
	    // String progress = "[green]Until the blue wins: [red]" + winScore;
	    winScore -= Vars.state.serverPaused ? 0 : updateInterval;
	    Groups.player.each(p -> Call.infoPopup(p.con(), L10NBundle.format("server.progress", findLocale(p.locale), winScore), updateInterval, Align.bottom, 0, 0, 0, 0));
	    if(winScore<1){
		Events.fire(new EventType.GameOverEvent(Team.blue));
		sendToChat("server.blue-won");
	    }
	}, 0, updateInterval);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("changeteam", "Change team - 1 time per game.", (args, player) -> {
            if (cooldowns.contains(player.uuid())) {
                bundled(player, "commands.team.cooldown");
                return;
            }
            if(player.team() == Team.green) {
                player.team(Team.blue);
                bundled(player, "commands.team.changed.blue");
                cooldowns.add(player.uuid());
                player.unit().kill();
            } else if (player.team() == Team.blue) {
                player.team(Team.green);
                bundled(player, "commands.team.changed.green");
                cooldowns.add(player.uuid());
                player.unit().kill();
            } else {
                bundled(player, "commands.team.error");
            }
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
}
