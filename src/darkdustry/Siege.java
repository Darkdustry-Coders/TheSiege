package darkdustry;

import arc.Events;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.game.EventType;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;

import static mindustry.Vars.netServer;

public class Siege extends Plugin {
    public static final Seq<String> cooldowns = new Seq<>();

    public static int winScore = 1500;
    public static Logic logic;
    public static Rules rules;

    public void init() {
        rules = new Rules();
        rules.pvp = true;
        rules.canGameOver = false;
        rules.waves = true;
        rules.waveTimer = false;
        rules.waveSpacing = 30 * Time.toMinutes;
        rules.polygonCoreProtection = true;
        rules.revealedBlocks.addAll(Blocks.duct, Blocks.ductRouter, Blocks.ductBridge, Blocks.thruster, Blocks.blockForge, Blocks.blockLoader, Blocks.blockUnloader, Blocks.scrapWall, Blocks.scrapWallLarge, Blocks.scrapWallHuge, Blocks.scrapWallGigantic);
        rules.bannedBlocks.addAll(Blocks.foreshadow);

        netServer.admins.addActionFilter((action) -> {
            if (action.player.team() == Team.green) {
                return !(action.block instanceof Turret && !(action.block == Blocks.wave));
            } else {
                return !(action.block instanceof UnitFactory || action.block instanceof Reconstructor);
            }
        });

        Events.on(EventType.WorldLoadEvent.class, event -> {
            winScore = 1500;
            cooldowns.clear();

            UnitTypes.poly.weapons.clear();
            Bullets.missileSurge.damage = 10.0f;
            ((ItemTurret)Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).damage = 750;
        });

        logic = new Logic();
        Timer.schedule(() -> logic.update(), 0, 1);

        Events.on(EventType.ServerLoadEvent.class, e -> {
            logic.restart();
            Log.info("[Darkdustry] The Siege loaded. Hosting a server...");
            netServer.openServer();
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("changeteam", "Change team - once per game.", (args, player) -> {
            if (cooldowns.contains(player.uuid())) {
                Bundle.bundled(player, "commands.team.cooldown");
                return;
            }
            Team team = player.team() == Team.green ? Team.blue : Team.green;
            player.team(team);
            Bundle.bundled(player, "commands.team.changed", colorizedTeam(team));
            cooldowns.add(player.uuid());
            if (player.unit() != null) player.unit().kill();
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        // Breaks the game
        handler.removeCommand("gameover");
        handler.register("gameover", "End the game.", args -> logic.endGame(Team.green));
    }

    public static String colorizedTeam(Team team) {
        return Strings.format("[#@]@", team.color, team);
    }
}
