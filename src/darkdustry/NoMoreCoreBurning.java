package darkdustry;

import arc.util.Timer;
import mindustry.Vars;
import mindustry.entities.Fires;
import mindustry.game.Teams.*;
import mindustry.gen.Fire;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.tilesize;

public class NoMoreCoreBurning extends Plugin {

    @Override
    public void init() {
        Timer.schedule(() -> {
            for (TeamData team : Vars.state.teams.active) {
                for (CoreBuild core : team.cores) {
                    if (Fires.has(core.tile.x, core.tile.y)) {
                        Groups.fire.each(fire -> fire.dst2(core) < 4 * tilesize * tilesize * core.block.size * core.block.size, Fire::remove);
                    }
                }
            }
        }, 0, 1f);
    }
}
