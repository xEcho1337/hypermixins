package net.echo.hypermixins.agent;

import net.echo.hypermixins.api.Mixin;
import net.echo.hypermixins.api.Original;
import net.echo.hypermixins.api.Overwrite;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class MixinMapping {
    
    public final Class<?> mixinClass;
    public final String targetClass;
    public final Map<String, Method> overwrites;
    public final Map<String, String> originals = new HashMap<>();
    
    public MixinMapping(Class<?> mixinClass) {
        this.mixinClass = mixinClass;
        
        Mixin mixin = mixinClass.getAnnotation(Mixin.class);
        
        if (mixin == null) throw new IllegalArgumentException("Missing @Mixin on " + mixinClass);
        
        this.targetClass = mixin.value();
        this.overwrites = new HashMap<>();
        
        for (Method method : mixinClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Original.class)) {
                Original origAnn = method.getAnnotation(Original.class);
                String mixinKey = method.getName() + Type.getMethodDescriptor(method);
                String targetName = origAnn.value().isEmpty() ? method.getName() : origAnn.value();
                originals.put(mixinKey, targetName);
                continue;
            }
            
            Overwrite overwrite = method.getAnnotation(Overwrite.class);
            
            if (overwrite == null) continue;
            
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
            
            String key = method.getName() + targetDescriptor(method);
            
            if (overwrites.containsKey(key)) {
                throw new IllegalStateException("Duplicate @Overwrite for " + key + " in " + mixinClass);
            }
            
            overwrites.put(key, method);
        }
    }
    
    private static String targetDescriptor(Method mixinMethod) {
        Type returnType = Type.getReturnType(mixinMethod);
        Type[] args = Type.getArgumentTypes(mixinMethod);
        Type[] targetArgs = Arrays.copyOfRange(args, Math.min(1, args.length), args.length);
        
        return Type.getMethodDescriptor(returnType, targetArgs);
    }
}
