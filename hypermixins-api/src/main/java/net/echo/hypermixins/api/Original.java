package net.echo.hypermixins.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method as a call to the original implementation
 * of a target method.
 * <p>
 * The method must declare {@code Object self} as its first parameter.
 *
 * @author xEcho1337
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Original {
    
    /**
     * Name of the target method.
     */
    String value();
}
