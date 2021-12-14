package darkdustry;

import arc.Events;
import arc.util.Align;
import arc.util.Strings;
import mindustry.content.Items;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;

import static mindustry.Vars.content;
import static mindustry.Vars.state;

public class Logic {
    public static void update() {
        if (!state.serverPaused) {
            Siege.winScore--;
            state.teams.active.each(team -> team.core() != null, team -> content.items().each(item -> item != Items.blastCompound, item -> team.core().items.add(item, team.cores.size * 50)));
        }

        Groups.player.each(p -> Call.infoPopup(p.con(), Bundle.format("labels.timer", Bundle.findLocale(p), Siege.winScore), 1, Align.bottom, 0, 0, 0, 0));
        if (Siege.winScore < 1) {
            Siege.winScore = 15000;
            Bundle.sendToChat("events.win.blue");
            Events.fire(new EventType.GameOverEvent(Team.blue));
        }
    }

    public static String colorizedTeam(Team team) {
        return Strings.format("[#@]@", team.color, team);
    }
}
