package net.echo.hypermixins;

import net.echo.hypermixins.transformer.MixinTransformer;

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
            
            // Transforms all the already-loaded classes
            for (Class<?> loaded : inst.getAllLoadedClasses()) {
                if (!inst.isModifiableClass(loaded)) continue;
                
                for (MixinMapping mapping : mappings) {
                    if (loaded == mapping.targetClass) {
                        inst.retransformClasses(loaded);
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
