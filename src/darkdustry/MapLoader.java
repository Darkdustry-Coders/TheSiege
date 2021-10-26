package darkdustry;

import arc.func.Cons;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.Gamemode;
import mindustry.maps.Map;
import mindustry.world.Tiles;

public class MapLoader implements Cons<Tiles> {
    int width, height;
    Tiles saved;

    public MapLoader() {
        Map map = Vars.maps.getNextMap(Gamemode.pvp, Vars.state.map);
        Log.info("Загружаю карту @ (@)", map.name(), map.file.nameWithoutExtension());
        Vars.world.loadMap(map, Siege.rules.copy());
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
