package net.echo.tests;

import net.echo.hypermixins.Mixin;
import net.echo.hypermixins.Overwrite;
import net.echo.testworld.Player;
import net.echo.testworld.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(World.class)
public class WorldMixin {
    
    private static Map<World, List<Player>> injectedPlayers = new HashMap<>();
    
    @Overwrite
    public static List<Player> getPlayers(World self) {
        return injectedPlayers.computeIfAbsent(self, k -> new ArrayList<>());
    }
}
