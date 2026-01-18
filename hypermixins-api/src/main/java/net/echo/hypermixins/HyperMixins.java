package net.echo.hypermixins;

import net.echo.hypermixins.agent.MixinMapping;
import net.echo.hypermixins.agent.MixinTransformer;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for registering and applying HyperMixins through the Java Instrumentation API.
 * <p>
 * This class is intended to be used exclusively from within a Java agent
 * (typically inside the {@code premain} method).
 * It wires user-defined mixin classes to their target classes by installing
 * a {@link net.echo.hypermixins.agent.MixinTransformer} and triggering class retransformation.
 *
 * @author xEcho1337
 * @apiNote This API operates at bytecode level and relies on class retransformation.
 *          Calling it outside an agent context or after application startup
 *          may lead to undefined behavior or {@link UnsupportedOperationException}s.
 */
public class HyperMixins {
    
    /**
     * Registers one or more mixin classes and applies them via the provided
     * {@link Instrumentation} instance.
     * <p>
     * Each mixin class is analyzed and converted into a {@link MixinMapping},
     * which defines target classes, overwritten methods, and original method bindings.
     * A single {@link MixinTransformer} is then installed to handle bytecode rewriting.
     * <p>
     * This method performs class retransformation and therefore requires the
     * instrumentation instance to support retransformation.
     *
     * @param inst the {@link Instrumentation} instance provided by the Java agent
     * @param mixinClasses one or more classes annotated as mixins to be applied
     * @throws RuntimeException if mixin analysis, transformer installation, or class retransformation fails
     * @implNote
     *     This method must be invoked from {@code premain}.
     *     Invoking it multiple times or after classes have already been transformed
     *     may result in duplicate transformations or unexpected behavior.
     */
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
