package net.echo.hypermixins.transformer;

import net.echo.hypermixins.MixinMapping;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixinTransformer implements ClassFileTransformer {
    
    private final Map<String, MixinMapping> targets = new HashMap<>();
    
    public MixinTransformer(List<MixinMapping> mappings) {
        for (MixinMapping m : mappings) {
            String internal = m.targetClass.getName().replace('.', '/');
            targets.put(internal, m);
        }
    }
    
    @Override
    public byte[] transform(
        Module module,
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer
    ) {
        MixinMapping mapping = targets.get(className);
        if (mapping == null) return null;
        
        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode node = new ClassNode();
            
            reader.accept(node, 0);
            
            for (var method : node.methods) {
                String key = method.name + method.desc;
                Method overwrite = mapping.overwrites.get(key);
                
                if (overwrite == null) continue;
                
                applyOverwrite(node.name, method, overwrite);
            }
            
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            
            return writer.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            return null;
        }
    }
    
    private static void applyOverwrite(
        String ownerInternal,
        MethodNode target,
        Method mixinMethod
    ) {
        target.instructions.clear();
        target.tryCatchBlocks.clear();
        
        InsnList insns = new InsnList();
        insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        
        String mixinOwner = Type.getInternalName(mixinMethod.getDeclaringClass());
        String desc = Type.getMethodDescriptor(mixinMethod);
        
        insns.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            mixinOwner,
            mixinMethod.getName(),
            desc,
            false
        ));
        
        insns.add(new InsnNode(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN)));
        
        target.instructions.add(insns);
    }
}
