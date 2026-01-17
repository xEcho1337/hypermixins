package net.echo.hypermixins;

import net.echo.hypermixins.transformer.MixinTransformer;

import java.lang.instrument.Instrumentation;

public class HyperMixins {

    public static void register(Instrumentation inst, Class<?>... classes) {
        inst.addTransformer(new MixinTransformer(classes), true);
    }
}
