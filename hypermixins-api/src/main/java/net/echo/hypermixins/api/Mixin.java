package net.echo.hypermixins.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a class as a mixin targeting an existing class.
 * <p>
 * Methods annotated with {@link Overwrite} or {@link Original} will be
 * applied to the specified target class at runtime.
 *
 * @author xEcho1337
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mixin {
    
    /**
     * Fully qualified binary name of the target class.
     */
    String value();
}

