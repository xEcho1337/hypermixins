package net.echo.hypermixins.agent;

import net.echo.hypermixins.api.Call;

import java.lang.reflect.Method;

public record RedirectMapping(String targetMethod, String invokeDesc, int index, Call call, Method handler) {
}
