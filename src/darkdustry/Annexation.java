package darkdustry;

import arc.Events;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.world.blocks.storage.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.util.Collections.reverse;

public class Annexation extends Plugin {
    HashMap<Team, Integer> scores = new HashMap<>();
    HashMap<Team, Integer> lastIncrease = new HashMap<>();

    int winScore = 25000;
    int updateInterval = 5;
    int topLength = 5;

    @Override
    public void init() {

        Vars.state.rules.coreCapture = true;

        Timer.schedule(() -> {

            if (!Vars.state.serverPaused) {
                Seq<TeamData> teams = Vars.state.teams.present;
                for (TeamData team : teams) {
                    int scoreIncrease = 0;
                    if (team.team == Team.purple) continue;
                    for (Building core : team.cores) {
                        scoreIncrease += core.block.size;
                    }
                    lastIncrease.put(team.team, scoreIncrease);
                    scores.put(team.team, scores.getOrDefault(team.team, 0) + scoreIncrease);
                }

                Map.Entry<Team, Integer> maxScore = null;
                for (Map.Entry<Team, Integer> score : scores.entrySet()) {
                    if (maxScore == null || score.getValue() > maxScore.getValue()) {
                        maxScore = score;
                    }
                }

                if (maxScore != null) {
                    int bestScore = maxScore.getValue();
                    if (bestScore > winScore) {
                        Team winner = maxScore.getKey();
                        Events.fire(new EventType.GameOverEvent(winner));
                    }
                }

                scores.entrySet().removeIf(entry -> !entry.getKey().active());
                lastIncrease.entrySet().removeIf(entry -> !entry.getKey().active());

                List<Map.Entry<Team, Integer>> list = new ArrayList<>(scores.entrySet());
                list.sort(Map.Entry.comparingByValue());
                reverse(list);

                String progress = "Очки для победы: " + winScore;

                int count = 0;
                for (Map.Entry<Team, Integer> entry : list) {
                    count++;
                    if(count > topLength) break;
                    Team team = entry.getKey();
                    int score = entry.getValue();
                    progress += "\n#" + count + " [#" + team.color.toString() + "]" + team.name + " : " + score + " + " + lastIncrease.getOrDefault(team, 0) + "[]";
                }

                Call.infoPopup(progress, updateInterval, Align.bottom, 0, 0, 0, 0);
            }

            if (scores.size() == 1) {
                Team winner = scores.keySet().stream().findFirst().get();
                Events.fire(new EventType.GameOverEvent(winner));
                Call.sendMessage("Игра окончена! Победила команда: [#" + winner.color.toString() + "]" + winner.name);
            }

        }, 0, updateInterval);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            scores.clear();
            lastIncrease.clear();
        });

        Events.on(BlockDestroyEvent.class, event -> {
            if(event.tile.block() instanceof CoreBlock) {
                Groups.player.each(player -> player.snapSync());
            }
        });
      
        Timer.schedule(() -> {

            HashMap<Team, Integer> amount = new HashMap<>();
            for (Teams.TeamData team : Vars.state.teams.active) {
                if (team.team == Team.purple) continue;
                amount.put(team.team, 0);
            }

            ArrayList<Player> players = new ArrayList<>();
            Groups.player.each(player -> {
                if (player.team() == Team.purple) {
                    players.add(player);
                } else {
                    int count = amount.getOrDefault(player.team(), 0);
                    amount.put(player.team(), count + 1);
                }
            });

            if (players.size() > 0) {

                List<Map.Entry<Team, Integer>> list = new ArrayList<>(amount.entrySet());
                list.sort(Map.Entry.comparingByValue());

                for (Player player : players) {

                    Map.Entry<Team, Integer> lastEntry = list.get(0);
                    int index = -1;
                    for (Map.Entry<Team, Integer> entry : list) {
                        index++;
                        if (lastEntry.getValue() < entry.getValue()) {
                            break;
                        }
                        lastEntry = entry;
                    }

                    Map.Entry<Team, Integer> newEntry = new AbstractMap.SimpleEntry<>(lastEntry.getKey(), lastEntry.getValue() + 1);
                    list.set(index,  newEntry);
                    player.team(lastEntry.getKey());
                }
            }
        }, 0, 1);
    }
}
