package darkdustry;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;

import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

public class Siege extends Plugin {
    public static final Seq<String> cooldowns = new Seq<>();

    public static int winScore = 1500;

    public void init() {
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

            UnitTypes.poly.weapons.clear();
            Bullets.missileSurge.damage = 8f;
            ((ItemTurret)Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).damage = 750f;
        });

        Timer.schedule(Logic::update, 0f, 1f);

        Log.info("[Darkdustry] The Siege loaded...");
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("changeteam", "commands.changeteam.description", (args, player) -> {
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
