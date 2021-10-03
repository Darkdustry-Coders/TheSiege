package darkdustry;

import arc.Events;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.content;
import static mindustry.Vars.state;
public class Logic {
    public boolean worldLoaded = false;

    public Logic() {
        Events.on(EventType.BlockDestroyEvent.class, event -> {
            if (event.tile != null && event.tile.build instanceof CoreBlock.CoreBuild && event.tile.build.team.cores().size <= 1 && worldLoaded && event.tile.build.team() == Team.blue) endGame(Team.green);
        });
    }

    public void update() {
        if (!worldLoaded) return;

        if (!state.serverPaused) {
            state.teams.active.each(team -> team.core() != null, team -> content.items().each(item -> item != Items.blastCompound, item -> team.core().items.add(item, team.cores.size * 100)));
        }

        Siege.winScore -= state.serverPaused ? 0 : 1;
        Groups.player.each(p -> Call.infoPopup(p.con(), Bundle.format("server.progress", Bundle.findLocale(p), Siege.winScore), 1, Align.bottom, 0, 0, 0, 0));
        if (Siege.winScore < 1) {
            Siege.winScore = 15000;
            endGame(Team.blue);
        }
    }

    public void restart() {
        Seq<Player> players = new Seq<>();
        Groups.player.copy(players);

        Vars.logic.reset();
        Call.worldDataBegin();

        MapLoader loader = new MapLoader();
        loader.run();

        players.each(player -> {
            Vars.netServer.assignTeam(player, players);
            Vars.netServer.sendWorldData(player);
        });

        Vars.logic.play();

        Timer.schedule(() -> worldLoaded = true, 7.5f);
    }

    public void endGame(Team team) {
        Groups.player.each(p -> Call.infoMessage(p.con(), Bundle.format(team == Team.blue ? "events.win.blue" : "events.win.green", Bundle.findLocale(p))));
        Timer.schedule(this::restart, 7.5f);
        worldLoaded = false;
    }
}
