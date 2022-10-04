package com.github.veluvexiau.umldoc.core;

import org.objectweb.asm.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
       /* if (args.length<1){
            throw new IllegalStateException("no path spécified");
        }*/
        var path = Path.of("target");
        var finder = ModuleFinder.of(path);
        PrintWriter writer = new PrintWriter("mermaid.md", Charset.defaultCharset());


        for(var moduleReference: finder.findAll()) {
            try(var reader = moduleReference.open()) {
                for(var filename: (Iterable<String>) reader.list()::iterator) {
                    if (!filename.endsWith(".class")) {
                        continue;
                    }
                    try(var inputStream = reader.open(filename).orElseThrow()) {
                        var classReader = new ClassReader(inputStream);
                        classReader.accept(new ClassVisitor(Opcodes.ASM9) {

                            private static String modifier(int access) {
                                if (Modifier.isPublic(access)) {
                                    return "+";
                                }
                                if (Modifier.isPrivate(access)) {
                                    return "-";
                                }
                                if (Modifier.isProtected(access)) {
                                    return "#";
                                }
                                return "";
                            }

                            @Override
                            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                writer.println("class " + modifier(access) + " " + name + " " + superName + " " + (interfaces != null? Arrays.toString(interfaces): ""));
                            }

                            @Override
                            public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
                                writer.println("  component " + name + " " + ClassDesc.ofDescriptor(descriptor).displayName());
                                return null;
                            }

                            @Override
                            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                writer.println("  field " + modifier(access) + " " + name + " " + ClassDesc.ofDescriptor(descriptor).displayName() + " " + signature);
                                return null;
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                writer.println("  method " + modifier(access) + " " + name + " " + MethodTypeDesc.ofDescriptor(descriptor).displayDescriptor() + " " + signature);
                                return null;
                            }
                        }, 0);
                    }
                }
            }
        }
        writer.close();

    }
}
