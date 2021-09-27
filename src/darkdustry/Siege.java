package darkdustry;

import arc.Events;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.game.EventType;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.ui.Menus;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;

import java.util.Locale;

import static mindustry.Vars.*;

public class Siege extends Plugin {
    private final Seq<String> cooldowns = new Seq<>();

    private int winScore = 1500;

    public void init() {

        netServer.admins.addActionFilter((action) -> {
            if (action.player.team() == Team.blue) {
                if (action.type == Administration.ActionType.placeBlock && action.block == Blocks.foreshadow && Groups.build.count(build -> build.team == Team.blue && build.block == Blocks.foreshadow) > foreshadowLimit) {
                    bundled(action.player, "server.foreshadow-limit", foreshadowLimit);
                    return false;
                }
                return !(action.block instanceof UnitFactory || action.block instanceof Reconstructor);
            } else return !(action.block instanceof Turret && !(action.block == Blocks.wave));
        });

        Events.on(WorldLoadEvent.class, event -> {
            winScore = 1500;
            cooldowns.clear();
            state.rules.unitDamageMultiplier = 2.0F;
            state.rules.unitBuildSpeedMultiplier = 2.0F;
            state.rules.blockDamageMultiplier = 0.6F;
            UnitTypes.poly.weapons.clear();
            Bullets.missileSurge.damage = 10.0F;
            ((ItemTurret)Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).damage = 750;
        });

        Events.on(PlayerJoin.class, event -> {
            if (netServer.admins.getInfo(event.player.uuid()).timesJoined <= 1) {
                String[][] optionsFirst = {{Bundle.format("server.first-join.yes", findLocale(event.player))}, {Bundle.format("server.first-join.no", findLocale(event.player))}};
                Call.menu(event.player.con, 1, Bundle.format("server.first-join.header", findLocale(event.player)), Bundle.format("server.first-join.content", findLocale(event.player)), optionsFirst);
            }
        });

	Timer.schedule(() -> {
	    if (!state.serverPaused) {
	        state.teams.active.each((team) -> team.core() != null, (team) -> content.items().each((item) -> team.core().items.add(item, team.cores.size * 100)));
	    }

	    winScore -= state.serverPaused ? 0 : 1;
	    Groups.player.each(p -> Call.infoPopup(p.con(), Bundle.format("server.progress", findLocale(p), winScore), 1, Align.bottom, 0, 0, 0, 0));
	    if (winScore < 1) {
                winScore = 15000;
                sendToChat("server.blue-won");
		Events.fire(new EventType.GameOverEvent(Team.blue));
	    }
	}, 0, 1);

        Menus.registerMenu(1, (player, selection) -> {
            if (selection == 0) {
                String[][] options = {{Bundle.format("server.tutorial.yes", findLocale(player))}, {Bundle.format("server.tutorial.no", findLocale(player))}};
                Call.menu(player.con, 2, Bundle.format("server.tutorial-1.header", findLocale(player)), Bundle.format("server.tutorial-1.content", findLocale(player)), options);
            }
        });

        Menus.registerMenu(2, (player, selection) -> {
            if (selection == 0) {
                String[][] options = {{Bundle.format("server.tutorial.yes", findLocale(player))}, {Bundle.format("server.tutorial.no", findLocale(player))}};
                Call.menu(player.con, 3, Bundle.format("server.tutorial-2.header", findLocale(player)), Bundle.format("server.tutorial-2.content", findLocale(player)), options);
            }
        });

        Menus.registerMenu(3, (player, selection) -> {
            if (selection == 0) {
                String[][] options = {{Bundle.format("server.tutorial.yes", findLocale(player))}, {Bundle.format("server.tutorial.no", findLocale(player))}};
                Call.menu(player.con, 4, Bundle.format("server.tutorial-3.header", findLocale(player)), Bundle.format("server.tutorial-3.content", findLocale(player)), options);
            }
        });

        Menus.registerMenu(4, (player, selection) -> {
            if (selection == 0) {
                String[][] options = {{Bundle.format("server.tutorial.yes", findLocale(player))}, {Bundle.format("server.tutorial.no", findLocale(player))}};
                Call.menu(player.con, 5, Bundle.format("server.tutorial-4.header", findLocale(player)), Bundle.format("server.tutorial-4.content", findLocale(player)), options);
            }
        });

        Menus.registerMenu(5, (player, selection) -> {
            if (selection == 0) {
                String[][] optionFinal = {{Bundle.format("server.tutorial-final", findLocale(player))}};
                Call.menu(player.con, 6, Bundle.format("server.tutorial-5.header", findLocale(player)), Bundle.format("server.tutorial-5.content", findLocale(player)), optionFinal);
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

        handler.<Player>register("info", "Information about gamemode.", (args, player) -> Call.menuChoose(player, 1, 0));
    }

    // Различные функции, выполняемые в коде.

    private static Locale findLocale(Player player) {
        Locale locale = Structs.find(Bundle.supportedLocales, l -> l.toString().equals(player.locale) || player.locale.startsWith(l.toString()));
        return locale != null ? locale : Bundle.defaultLocale();
    }

    public static void sendToChat(String key, Object... values) {
        Groups.player.each(p -> bundled(p, key, values));
    }

    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(Bundle.format(key, findLocale(player), values));
    }

    public static String colorizedTeam(Team team){
        return Strings.format("[#@]@", team.color, team);
    }
}
