package net.echo.tests;

import net.echo.hypermixins.api.Mixin;
import net.echo.hypermixins.api.Overwrite;

@Mixin("net.echo.testworld.Player")
public class PlayerMixin {
    
    @Overwrite
    public String getName(Object self) {
        return "Foobar";
    }
}
