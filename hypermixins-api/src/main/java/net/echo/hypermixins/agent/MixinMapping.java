package net.echo.hypermixins.agent;

import net.echo.hypermixins.api.*;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class MixinMapping {
    
    public final Class<?> mixinClass;
    public final String targetClass;
    public final Map<String, Method> overwrites; // mixin method name -> redirected method
    public final Map<String, String> originals; // mixin method name -> original method name
    public final List<RedirectMapping> redirects;
    
    public MixinMapping(Class<?> mixinClass) {
        this.mixinClass = mixinClass;
        
        Mixin mixin = mixinClass.getAnnotation(Mixin.class);
        
        if (mixin == null) throw new IllegalArgumentException("Missing @Mixin on " + mixinClass);
        
        this.targetClass = mixin.value();
        this.overwrites = new HashMap<>();
        this.originals = new HashMap<>();
        this.redirects = new ArrayList<>();
        
        for (Method method : mixinClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Original.class)) {
                handleOriginal(method);
            } else if (method.isAnnotationPresent(Overwrite.class)) {
                handleOverwrite(method, mixin);
            } else if (method.isAnnotationPresent(Redirect.class)) {
                handleRedirect(method, mixin);
            }
        }
    }
    
    private void handleOriginal(Method method) {
        Original original = method.getAnnotation(Original.class);
        
        if (original.value().isEmpty()) {
            throw new IllegalArgumentException("Value inside @Original on " + method + " is empty!");
        }
        
        String mixinKey = method.getName() + Type.getMethodDescriptor(method);
        originals.put(mixinKey, original.value());
    }
    
    private void handleOverwrite(Method method, Mixin mixin) {
        Overwrite overwrite = method.getAnnotation(Overwrite.class);
        
        if (overwrite.value().isEmpty()) {
            throw new IllegalArgumentException("Value inside @Overwrite on " + method + " is empty!");
        }
        
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("@Overwrite on static methods is not supported yet! Method: " + method);
        }
        
        Class<?>[] parameterTypes = method.getParameterTypes();
        
        if (parameterTypes.length == 0) {
            throw new IllegalStateException("Missing Object self on overwritten method: " + method.getName());
        }
        
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType.getName().equals(mixin.value())) {
                throw new IllegalStateException(
                    """
                    Overwritten methods cannot contain a reference to the target class!
                    Use "Object self" instead and cast the object later
                    """
                );
            }
        }
        
        String key = overwrite.value() + targetDescriptor(method);
        
        if (overwrites.containsKey(key)) {
            throw new IllegalStateException("Duplicate @Overwrite for " + key + " in " + mixinClass);
        }
        
        overwrites.put(key, method);
    }
    
    private void handleRedirect(Method method, Mixin mixin) {
        Redirect redirect = method.getAnnotation(Redirect.class);
        At at = redirect.at();
        
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("@Redirect methods must be static: " + method);
        }
        
        if (at.desc().isEmpty()) {
            throw new IllegalArgumentException("Missing desc in @At for @Redirect on " + method);
        }
        
        if (redirect.method().isEmpty()) {
            throw new IllegalArgumentException("Missing target method in @Redirect on " + method);
        }
        
        if (at.index() < 0) {
            throw new IllegalArgumentException("Negative index in @Redirect on " + method);
        }
        
        String handlerDesc = Type.getMethodDescriptor(method);
        
        // signature INVOKE target
        // es: java/lang/Thread.sleep(J)V
        int paren = at.desc().indexOf('(');
        
        if (paren == -1) {
            throw new IllegalArgumentException("Invalid invoke desc in @At: " + at.desc());
        }
        
        String invokeDesc = at.desc().substring(paren);
        
        if (!handlerDesc.equals(invokeDesc)) {
            throw new IllegalStateException(
                """
                Redirect method signature does not match INVOKE signature!
                Method: %s
                Expected: %s
                Found: %s
                """.formatted(method, invokeDesc, handlerDesc)
            );
        }
        
        redirects.add(new RedirectMapping(
            redirect.method(),
            at.desc(),
            at.index(),
            at.call(),
            method
        ));
    }
    
    private static String targetDescriptor(Method mixinMethod) {
        Type returnType = Type.getReturnType(mixinMethod);
        Type[] args = Type.getArgumentTypes(mixinMethod);
        Type[] targetArgs = Arrays.copyOfRange(args, Math.min(1, args.length), args.length);
        
        return Type.getMethodDescriptor(returnType, targetArgs);
    }
}
