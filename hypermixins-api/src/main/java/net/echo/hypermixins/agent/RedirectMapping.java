package net.echo.hypermixins.agent;

import java.lang.reflect.Method;

public record RedirectMapping(String targetMethod, String invokeDesc, int index, Method handler) {
}
