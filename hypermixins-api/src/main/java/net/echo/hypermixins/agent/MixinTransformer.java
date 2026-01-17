package net.echo.hypermixins.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;

public class MixinTransformer implements ClassFileTransformer {
    
    private final Map<String, MixinMapping> targets = new HashMap<>();
    private final Map<String, MixinMapping> mixins = new HashMap<>();
    
    public MixinTransformer(List<MixinMapping> mappings) {
        for (MixinMapping m : mappings) {
            targets.put(m.targetClass.replace('.', '/'), m);
            mixins.put(Type.getInternalName(m.mixinClass), m);
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
        try {
            MixinMapping mapping;
            
            if ((mapping = targets.get(className)) != null) {
                return transformTarget(classfileBuffer, mapping);
            }
            
            if ((mapping = mixins.get(className)) != null) {
                return transformMixin(classfileBuffer, mapping);
            }
            return null;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            return null;
        }
    }
    
    private byte[] transformMixin(byte[] classfile, MixinMapping mapping) {
        ClassReader reader = new ClassReader(classfile);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        
        for (MethodNode method : node.methods) {
            String key = method.name + method.desc;
            
            if (!mapping.originals.containsKey(key)) continue;
            
            Type[] args = Type.getArgumentTypes(method.desc);
            if (args.length == 0) {
                throw new IllegalStateException("@Original method must declare Object self as first parameter: " + method.name + method.desc);
            }
            
            Type returnType = Type.getReturnType(method.desc);
            Type[] targetArgs = Arrays.copyOfRange(args, 1, args.length);
            String targetDesc = Type.getMethodDescriptor(returnType, targetArgs);
            
            // native -> normal
            method.access &= ~Opcodes.ACC_NATIVE;
            
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            
            InsnList insns = new InsnList();
            
            // load self (first arg of method in mixin)
            insns.add(new VarInsnNode(Opcodes.ALOAD, 1));
            insns.add(new TypeInsnNode(
                Opcodes.CHECKCAST,
                mapping.targetClass.replace('.', '/')
            ));
            
            String targetName = mapping.originals.get(key);
            String originalName = "__original$" + targetName + "$" + Integer.toHexString(targetDesc.hashCode());
            
            insns.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                mapping.targetClass.replace('.', '/'),
                originalName,
                targetDesc,
                false
            ));
            
            insns.add(new InsnNode(
                Type.getReturnType(targetDesc).getOpcode(Opcodes.IRETURN)
            ));
            
            method.instructions.add(insns);
        }
        
        ClassWriter writer = new ClassWriter(
            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS
        );
        node.accept(writer);
        return writer.toByteArray();
    }
    
    
    private byte[] transformTarget(byte[] classfile, MixinMapping mapping) {
        ClassReader reader = new ClassReader(classfile);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        
        String mixinField = "__mixin$" + mapping.mixinClass.getName().replace('.', '$');
        String mixinDesc = Type.getDescriptor(mapping.mixinClass);
        
        // add mixin field
        if (node.fields.stream().noneMatch(f -> f.name.equals(mixinField))) {
            node.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE,
                mixinField,
                mixinDesc,
                null,
                null
            ));
        }
        
        // init mixin in constructor
        for (MethodNode method : node.methods) {
            if (method.name.equals("<init>")) {
                patchConstructor(method, node, mapping, mixinField);
            }
        }
        
        List<MethodNode> originals = new ArrayList<>();
        
        // overwrites the requested methods
        for (MethodNode method : node.methods) {
            String key = method.name + method.desc;
            Method overwrite = mapping.overwrites.get(key);
            
            // if we don't need to overwrite this method
            if (overwrite == null) continue;
            
            // overwrite = replace BODY, not symbol
            applyOverwrite(node, method, overwrite, mixinField, originals);
        }
        
        node.methods.addAll(originals);
        
        ClassWriter writer = new ClassWriter(
            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS
        );
        node.accept(writer);
        return writer.toByteArray();
    }
    
    private static void patchConstructor(
        MethodNode ctor,
        ClassNode owner,
        MixinMapping mapping,
        String mixinFieldName
    ) {
        AbstractInsnNode insertAfter = null;
        
        for (AbstractInsnNode insn = ctor.instructions.getFirst();
             insn != null;
             insn = insn.getNext()) {
            
            if (insn instanceof MethodInsnNode mi
                && mi.getOpcode() == Opcodes.INVOKESPECIAL
                && mi.name.equals("<init>")
                && mi.owner.equals(owner.superName)) {
                
                insertAfter = insn;
                break;
            }
        }
        
        if (insertAfter == null) {
            throw new IllegalStateException("No super() call found in constructor of " + owner.name);
        }
        
        InsnList inject = new InsnList();
        
        inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inject.add(new TypeInsnNode(
            Opcodes.NEW,
            Type.getInternalName(mapping.mixinClass)
        ));
        inject.add(new InsnNode(Opcodes.DUP));
        inject.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            Type.getInternalName(mapping.mixinClass),
            "<init>",
            "()V",
            false
        ));
        inject.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            owner.name,
            mixinFieldName,
            Type.getDescriptor(mapping.mixinClass)
        ));
        
        ctor.instructions.insert(insertAfter, inject);
    }
    
    
    private static void applyOverwrite(
        ClassNode owner,
        MethodNode target,
        Method mixinMethod,
        String mixinFieldName,
        List<MethodNode> originals
    ) {
        MethodNode originalCopy = cloneAsOriginal(target);
        originals.add(originalCopy);
        
        target.instructions.clear();
        target.tryCatchBlocks.clear();
        target.localVariables = null;
        
        InsnList insns = new InsnList();
        
        // this.__mixin
        insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insns.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            owner.name,
            mixinFieldName,
            Type.getDescriptor(mixinMethod.getDeclaringClass())
        ));
        
        insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        
        Type[] targetArgs = Type.getArgumentTypes(target.desc);
        int localIndex = 1;
        
        for (Type t : targetArgs) {
            insns.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), localIndex));
            localIndex += t.getSize();
        }
        
        insns.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            Type.getInternalName(mixinMethod.getDeclaringClass()),
            mixinMethod.getName(),
            Type.getMethodDescriptor(mixinMethod),
            false
        ));
        
        insns.add(new InsnNode(Type.getReturnType(mixinMethod).getOpcode(Opcodes.IRETURN)));
        
        target.instructions.add(insns);
    }
    
    private static MethodNode cloneAsOriginal(MethodNode original) {
        String newName = "__original$" + original.name + "$" + Integer.toHexString(original.desc.hashCode());
        
        int acc = (original.access & Opcodes.ACC_STATIC) != 0
            ? (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC)
            : (Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC);
        
        MethodNode copy = new MethodNode(
            acc,
            newName,
            original.desc,
            original.signature,
            original.exceptions == null ? null : original.exceptions.toArray(new String[0])
        );
        
        original.accept(copy);
        return copy;
    }

}
