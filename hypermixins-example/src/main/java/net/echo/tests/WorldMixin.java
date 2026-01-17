package net.echo.tests;

import net.echo.hypermixins.api.Mixin;
import net.echo.hypermixins.api.Original;
import net.echo.hypermixins.api.Overwrite;
import net.echo.testworld.Player;

import java.util.ArrayList;
import java.util.List;

@Mixin("net.echo.testworld.World")
public class WorldMixin {
    
    private final List<Player> injectedPlayers = new ArrayList<>();
    
    @Original("getPlayers")
    public native List<Player> getPlayersOrig(Object self);
    
    @Overwrite
    public List<Player> getPlayers(Object self) {
        List<Player> original = getPlayersOrig(self);
        original.add(new Player("shelter"));
        return original;
    }
}
