package net.echo.hypermixins;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class MixinMapping {
    
    public final Class<?> mixinClass;
    public final Class<?> targetClass;
    public final Map<String, Method> overwrites;
    
    public MixinMapping(Class<?> mixinClass) {
        this.mixinClass = mixinClass;
        
        Mixin mixin = mixinClass.getAnnotation(Mixin.class);
        
        if (mixin == null) throw new IllegalArgumentException("Missing @Mixin on " + mixinClass);
        
        this.targetClass = mixin.value();
        this.overwrites = new HashMap<>();
        
        for (Method method : mixinClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalStateException("Overwrite must be static: " + method);
            }
            
            Overwrite overwrite = method.getAnnotation(Overwrite.class);
            
            if (overwrite == null) continue;
            
            String desc = MethodType
                .methodType(method.getReturnType(), method.getParameterTypes())
                .toMethodDescriptorString();
            
            String key = method.getName() + desc;
            
            if (overwrites.containsKey(key)) {
                throw new IllegalStateException("Duplicate @Overwrite for " + key + " in " + mixinClass);
            }
            
            overwrites.put(key, method);
        }
    }
}
