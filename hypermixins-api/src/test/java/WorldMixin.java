import net.echo.hypermixins.api.Mixin;
import net.echo.hypermixins.api.Overwrite;
import server.World;

import java.util.List;

@Mixin(World.class)
public class WorldMixin {

    @Overwrite(name = "getPlayers", desc = "java/util/List")
    public List<Integer> getPlayers(World self) {
        System.out.println("Overwritten!");
        return List.of(1, 2, 3);
    }
}
