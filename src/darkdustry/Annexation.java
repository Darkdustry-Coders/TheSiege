package darkdustry;

import arc.Events;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;

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

                String progress = "winscore is " + winScore;

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
                Events.fire(new EventType.GameOverEvent(scores.keySet().stream().findFirst().get()));
                Call.sendMessage("Игра окончена! Победила команда: "); //Вывод названия победившей команды
            }

        }, 0, updateInterval);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            scores.clear();
            lastIncrease.clear();
        });
    }
}
