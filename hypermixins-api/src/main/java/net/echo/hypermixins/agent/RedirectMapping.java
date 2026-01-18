package net.echo.hypermixins.agent;

import java.lang.reflect.Method;

/**
 * @param targetMethod es: "run"
 * @param invokeDesc   es: "java/lang/Thread.sleep(J)V"
 * @param index        ordinal
 * @param handler      static method of mixin
 */
public record RedirectMapping(String targetMethod, String invokeDesc, int index, Method handler) {
}
