package darkdustry;

import arc.Events;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.Locale;

import static mindustry.Vars.*;

public class Siege extends Plugin {
    private final Seq<String> cooldowns = new Seq<>();

    private int winScore = 1500;

    public void init() {

        netServer.admins.addActionFilter((action) -> {
            if (action.player.team() == Team.green) {
                return !(action.block instanceof Turret && !(action.block == Blocks.wave));
            } else {
                return !(action.block instanceof UnitFactory || action.block instanceof Reconstructor);
            }
        });

        Events.on(WorldLoadEvent.class, event -> {
            winScore = 1500;
            cooldowns.clear();

            UnitTypes.poly.weapons.clear();
            Bullets.missileSurge.damage = 10.0f;
            ((ItemTurret)Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).damage = 750;
        });

        Events.on(EventType.ServerLoadEvent.class, e -> {
            content.blocks().each(Objects::nonNull, block ->{
                if (block instanceof CoreBlock) block.health *= 0.75;
                else if (block instanceof Wall) block.health *= 0.5;
            });

            // TODO автозапуск сервера, своя логика геймовера.
            Log.info("[Darkdustry] The Siege loaded. Hosting a server...");
        });

	Timer.schedule(() -> {
	    if (!state.serverPaused) {
	        state.teams.active.each(team -> team.core() != null, team -> content.items().each(item -> item != Items.blastCompound, item -> team.core().items.add(item, team.cores.size * 100)));
	    }

	    winScore -= state.serverPaused ? 0 : 1;
	    Groups.player.each(p -> Call.infoPopup(p.con(), Bundle.format("server.progress", findLocale(p), winScore), 1, Align.bottom, 0, 0, 0, 0));
	    if (winScore < 1) {
                winScore = 15000;
                sendToChat("server.blue-won");
		Events.fire(new GameOverEvent(Team.blue));
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
            if (player.unit() != null) player.unit().kill();
        });
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
