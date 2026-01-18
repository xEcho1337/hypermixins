package net.echo.hypermixins.api;

public @interface At {
    String desc();
    int index() default 0;
}
