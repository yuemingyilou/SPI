package com.sunfusheng.spi.plugin


import org.objectweb.asm.*

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * @author by sunfusheng on 2019/3/19
 */
class ScanUtil {
    static final String ANNOTATION_PROVIDE_DESC = 'Lcom/sunfusheng/spi/api/Provide;'
    static int classesScanned = 0

    static boolean shouldProcessJarOrDir(String name) {
        return name != null && !name.startsWith('com.android.support') && !name.startsWith('android.arch')
    }

    static boolean shouldProcessClass(String name) {
        return name != null && name.endsWith('.class') &&
                !name.endsWith('BuildConfig.class') &&
                !name.startsWith('R$') &&
                name != 'R.class' &&
                name != 'com/sunfusheng/spi/api/Provide.class' &&
                name != 'com/sunfusheng/spi/api/ProvidersPool$1.class' &&
                name != 'com/sunfusheng/spi/api/ProvidersPool.class' &&
                name != 'com/sunfusheng/spi/api/ProvidersRegistry.class'
    }

    static void scanJar(File srcFile, File destFile) {
        if (srcFile) {
            def jarFile = new JarFile(srcFile)
            Enumeration<JarEntry> enumeration = jarFile.entries()
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement()
                String jarElementName = jarEntry.name
                if (jarElementName.startsWith(RegisterCodeGenerator.GENERATE_TO_CLASS_NAME)) {
                    println '【spi-plugin】Contains ServiceProvider.class JarPath: ' + destFile.absolutePath
                    RegisterCodeGenerator.mServiceProviderFile = destFile
                } else if (shouldProcessClass(jarElementName)) {
                    println '【spi-plugin】jarElementName: ' + jarElementName
                    InputStream inputStream = jarFile.getInputStream(jarEntry)
                    scanInputStream(inputStream)
                    inputStream.close()
                }
            }
            jarFile.close()
        }
    }

    static void scanFile(File file) {
        println '【spi-plugin】dirElementName: ' + file.name
        scanInputStream(new FileInputStream(file))
    }

    static void scanInputStream(InputStream inputStream) {
        classesScanned++
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ProvideClassVisitor cv = new ProvideClassVisitor(Opcodes.ASM6, cw)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        inputStream.close()
    }

    private static class ProvideClassVisitor extends ClassVisitor {
        def mClassName

        ProvideClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)
        }

        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
            mClassName = name
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible)
            if (desc.endsWith(ANNOTATION_PROVIDE_DESC)) {
                av = new ProvideAnnotationVisitor(Opcodes.ASM6, av, mClassName)
            }
            return av
        }
    }

    private static class ProvideAnnotationVisitor extends AnnotationVisitor {
        String mClassName

        ProvideAnnotationVisitor(int api, AnnotationVisitor av, String className) {
            super(api, av)
            mClassName = className
        }

        @Override
        void visit(String name, Object value) {
            super.visit(name, value)
            if (mClassName != null && mClassName.length() > 0 && value != null) {
                String key = ((String) value).replace('/', '.')
                if (key.startsWith('L')) {
                    key = key.substring(1, key.length() - 1)
                }
                String provider = mClassName.replace('/', '.')

                Set<String> providersSet = RegisterCodeGenerator.mProvidersMap.get(key)
                if (providersSet == null) {
                    providersSet = new HashSet<>()
                    RegisterCodeGenerator.mProvidersMap.put(key, providersSet)
                }
                providersSet.add(provider)
            }
        }
    }
}
