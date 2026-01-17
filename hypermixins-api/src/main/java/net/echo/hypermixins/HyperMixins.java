package net.echo.hypermixins;

import net.echo.hypermixins.agent.MixinMapping;
import net.echo.hypermixins.agent.MixinTransformer;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

public class HyperMixins {
    
    public static void register(
        Instrumentation inst,
        Class<?>... mixinClasses
    ) {
        try {
            List<MixinMapping> mappings = new ArrayList<>();
            
            for (Class<?> mixinClass : mixinClasses) {
                mappings.add(new MixinMapping(mixinClass));
            }
            
            MixinTransformer transformer = new MixinTransformer(mappings);
            
            inst.addTransformer(transformer, true);
            inst.retransformClasses(mixinClasses);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
