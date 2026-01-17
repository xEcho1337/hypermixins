package net.echo.hypermixins.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class MixinTransformer implements ClassFileTransformer {

    private final Class<?>[] classes;

    public MixinTransformer(Class<?>[] classes) {
        this.classes = classes;
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
        boolean isValid = false;

        for (Class<?> clazz : classes) {
            if (className.equals(clazz.getName())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) return null;

        System.out.println("Transforming " + className);

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode node = new ClassNode();

        reader.accept(node, 0);
        System.out.println("Methods:");
        node.methods.forEach(m ->
            System.out.println(" - " + m.name + m.desc)
        );

        return null;
    }
}
