package darkdustry;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.mod.Plugin;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;

import static mindustry.Vars.netServer;
import static mindustry.Vars.state;
import static mindustry.Vars.world;
import static mindustry.Vars.tilesize;

public class Siege extends Plugin {
    public static final Seq<String> cooldowns = new Seq<>();

    public static int winScore = 1500;

    public void init() {
        UnitTypes.poly.weapons.clear();

        netServer.admins.addActionFilter(action -> {
            if (action.player.team() == Team.green) {
                return !(action.block instanceof Turret && !(action.block == Blocks.wave));
            } else {
                return !(action.block instanceof UnitFactory || action.block instanceof Reconstructor);
            }
        });

        Events.on(EventType.WorldLoadEvent.class, event -> {
            winScore = 1500;
            cooldowns.clear();

            Core.app.post(() -> {
                state.rules.waves = false;
                state.rules.waveTimer = false;
                state.rules.polygonCoreProtection = true;
                state.rules.logicUnitBuild = false;
                state.rules.revealedBlocks.addAll(Blocks.duct, Blocks.ductRouter, Blocks.ductBridge, Blocks.thruster, Blocks.scrapWall, Blocks.scrapWallLarge, Blocks.scrapWallHuge, Blocks.scrapWallGigantic);
                state.rules.bannedBlocks.addAll(Blocks.constructor, Blocks.largeConstructor, Blocks.deconstructor, Blocks.swarmer);
                state.rules.teams.get(Team.blue).blockHealthMultiplier = 1.5f;
                state.rules.teams.get(Team.blue).buildSpeedMultiplier = 1.2f;
                state.rules.teams.get(Team.green).unitBuildSpeedMultiplier = 0.8f;
                state.rules.cleanupDeadTeams = true;
                Call.setRules(state.rules);
            });

            for (int i = 0; i < 8; i++) {
                Unit u = UnitTypes.poly.spawn(Team.blue, world.width() * tilesize / 2f, world.height() * tilesize / 2f);
                u.maxHealth = Integer.MAX_VALUE;
                u.health = u.maxHealth;
                u.armor = 0f;
            }
        });

        Timer.schedule(Logic::update, 0f, 1f);

        Events.on(EventType.ServerLoadEvent.class, e -> Log.info("[Darkdustry] The Siege loaded..."));
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("changeteam", "Change team - once per game.", (args, player) -> {
            if (cooldowns.contains(player.uuid())) {
                Bundle.bundled(player, "commands.team.cooldown");
                return;
            }
            Team team = player.team() == Team.green ? Team.blue : Team.green;
            player.clearUnit();
            player.team(team);
            cooldowns.add(player.uuid());
            Bundle.bundled(player, "commands.team.changed", Logic.colorizedTeam(team));
        });
    }
}
