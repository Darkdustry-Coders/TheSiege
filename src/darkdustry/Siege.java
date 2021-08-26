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
import mindustry.ui.Menus;
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
            if(netServer.admins.getInfo(event.player.uuid()).timesJoined <= 1) {
                String[][] optionsFirst = {
                        {Bundle.format("server.first-join.yes", findLocale(event.player.locale))},
                        {Bundle.format("server.first-join.no", findLocale(event.player.locale))}
                };
                Call.menu(event.player.con, 1, Bundle.format("server.first-join.header", findLocale(event.player.locale)), Bundle.format("server.first-join.content", findLocale(event.player.locale)), optionsFirst);
            }
        });

	Timer.schedule(() -> {
            if (!state.serverPaused) {
	        state.teams.active.each((team) -> {
                    return team.core() != null;
                }, (team) -> content.items().each((item) -> team.core().items.add(item, (int)team.cores.size * 100)));
            }

	    winScore -= state.serverPaused ? 0 : 1;
	    Groups.player.each(p -> Call.infoPopup(p.con(), Bundle.format("server.progress", findLocale(p.locale), winScore), 1, Align.bottom, 0, 0, 0, 0));
	    if(winScore<1){
                winScore = 15000;
                sendToChat("server.blue-won");
		Events.fire(new EventType.GameOverEvent(Team.blue));
	    }
	}, 0, 1);

        Menus.registerMenu(1, (player, selection) -> {
            if (selection == 0) {
                String[][] options = {
                    {Bundle.format("server.tutorial.yes", findLocale(player.locale))},
                    {Bundle.format("server.tutorial.no", findLocale(player.locale))}
                };
                Call.menu(player.con, 2, Bundle.format("server.tutorial-1.header", findLocale(player.locale)), Bundle.format("server.tutorial-1.content", findLocale(player.locale)), options);
            }
        });

        Menus.registerMenu(2, (player, selection) -> {
            if (selection == 0) {
                String[][] options = {
                    {Bundle.format("server.tutorial.yes", findLocale(player.locale))},
                    {Bundle.format("server.tutorial.no", findLocale(player.locale))}
                };
                Call.menu(player.con, 3, Bundle.format("server.tutorial-2.header", findLocale(player.locale)), Bundle.format("server.tutorial-2.content", findLocale(player.locale)), options);
            }
        });

        Menus.registerMenu(3, (player, selection) -> {
            if (selection == 0) {
                String[][] options = {
                    {Bundle.format("server.tutorial.yes", findLocale(player.locale))},
                    {Bundle.format("server.tutorial.no", findLocale(player.locale))}
                };
                Call.menu(player.con, 4, Bundle.format("server.tutorial-3.header", findLocale(player.locale)), Bundle.format("server.tutorial-3.content", findLocale(player.locale)), options);
            }
        });

        Menus.registerMenu(4, (player, selection) -> {
            if (selection == 0) {
                String[][] options = {
                    {Bundle.format("server.tutorial.yes", findLocale(player.locale))},
                    {Bundle.format("server.tutorial.no", findLocale(player.locale))}
                };
                Call.menu(player.con, 5, Bundle.format("server.tutorial-4.header", findLocale(player.locale)), Bundle.format("server.tutorial-4.content", findLocale(player.locale)), options);
            }
        });

        Menus.registerMenu(5, (player, selection) -> {
            if (selection == 0) {
                String[][] optionsFinal = {
                    {Bundle.format("server.tutorial-final", findLocale(player.locale))}
                };
                Call.menu(player.con, 6, Bundle.format("server.tutorial-5.header", findLocale(player.locale)), Bundle.format("server.tutorial-5.content", findLocale(player.locale)), optionsFinal);
            }
        });
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
            Call.menuChoose(player, 1, 0);
        });
    }

    // Различные функции, выполняемые в коде.

    private static Locale findLocale(String code) {
        Locale locale = Structs.find(Bundle.supportedLocales, l -> l.toString().equals(code) ||
                code.startsWith(l.toString()));
        return locale != null ? locale : Bundle.defaultLocale();
    }

    public static void sendToChat(String key, Object... values) {
        Groups.player.each(p -> p.sendMessage(Bundle.format(key, findLocale(p.locale), values)));
    }

    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(Bundle.format(key, findLocale(player.locale), values));
    }

    public static String colorizedTeam(Team team){
        return Strings.format("[#@]@", team.color, team);
    }
}
