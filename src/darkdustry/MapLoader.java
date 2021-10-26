package darkdustry;

import arc.func.Cons;
import mindustry.Vars;
import mindustry.game.Gamemode;
import mindustry.world.Tiles;

public class MapLoader implements Cons<Tiles> {
    int width, height;
    Tiles saved;

    public MapLoader() {
        Vars.world.loadMap(Vars.maps.getNextMap(Gamemode.pvp, null), Siege.rules.copy());
        saved = Vars.world.tiles;
        width = saved.width;
        height = saved.height;
    }

    public void run() {
        Vars.world.loadGenerator(width, height, this);
    }

    @Override
    public void get(Tiles t) {}
}
