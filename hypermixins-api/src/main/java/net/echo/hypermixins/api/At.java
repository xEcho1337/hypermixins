package net.echo.hypermixins.api;

public @interface At {
    String desc();
    Call call();
    int index() default 0;
}
