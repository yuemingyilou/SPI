package com.sunfusheng.spi.compiler;

import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import com.sunfusheng.spi.annotation.Provide;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * @author by sunfusheng on 2019/3/18
 */
public class ProvideCodeGenerator {

    public static final String PROJECT = "SPI";
    public static final String PROVIDER = "Provider";
    public static final String SEPARATOR = "$$";
    public static final String PROJECT_PROVIDER = PROJECT + SEPARATOR + PROVIDER + SEPARATOR;

    public static final String PACKAGE_OF_GENERATE_FILE = "com.sunfusheng.spi.providers";
    public static final String DOC_OF_GENERATE_FILE = "Do not edit this file! It was generated by SPI.";
    public static final String METHOD_REGISTER = "register";
    public static final String PATH_OF_SPI_API = "com.sunfusheng.spi.api";
    public static final String CLASS_PROVIDER_REGISTRY = "ProvidersRegistry";
    public static final String PARAM_REGISTRY = "registry";

    private static final ClassName providersRegistryClass = ClassName.get(PATH_OF_SPI_API, CLASS_PROVIDER_REGISTRY);

    private Filer mFiler;
    private Elements mElementUtils;
    private Messager mMessager;

    public ProvideCodeGenerator(ProcessingEnvironment processingEnv) {
        this.mFiler = processingEnv.getFiler();
        this.mElementUtils = processingEnv.getElementUtils();
        this.mMessager = processingEnv.getMessager();
    }

    public void generateCode(Set<? extends Element> elements) {
        printNote("### Found providers, size is " + elements.size());
        String packageName = null;

        ParameterSpec registryParamSpec = ParameterSpec.builder(providersRegistryClass, PARAM_REGISTRY).build();
        MethodSpec.Builder registerMethodSpec = MethodSpec.methodBuilder(METHOD_REGISTER)
                .addModifiers(PUBLIC, STATIC)
                .addParameter(registryParamSpec);

        for (Element element : elements) {
            if (filterElement(element)) {
                continue;
            }
            TypeElement typeElement = (TypeElement) element;
            packageName = getPublicPackageName(packageName, mElementUtils.getPackageOf(element).getQualifiedName().toString());
            String className = typeElement.getQualifiedName().toString();
            String annotationValue = getProviderAnnotationValue(element);
            printNote("### packageName: " + packageName);
            printNote("### className: " + className);
            printNote("### annotationValue: " + annotationValue);
            registerMethodSpec.addStatement("registry.register($S, $S)", annotationValue, className);
        }

        try {
            String generateFileName = PROJECT_PROVIDER + packageName;
            generateFileName = generateFileName.replaceAll("[^0-9a-zA-Z_$]+", "_");
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE, TypeSpec.classBuilder(generateFileName)
                    .addJavadoc(DOC_OF_GENERATE_FILE)
                    .addModifiers(PUBLIC)
                    .addMethod(registerMethodSpec.build())
                    .build()
            ).build().writeTo(mFiler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 过滤被注解的元素
    private boolean filterElement(Element element) {
        if (element != null && element.getKind() == ElementKind.CLASS && element.getAnnotation(Provide.class) != null) {
            return false;
        }
        return true;
    }

    // 获取注解值的全限定名
    private String getProviderAnnotationValue(Element element) {
        try {
            Provide provide = element.getAnnotation(Provide.class);
            Class<?> clazz = provide.value();
            return clazz.getCanonicalName();
        } catch (MirroredTypeException mte) {
            DeclaredType declaredType = (DeclaredType) mte.getTypeMirror();
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            return typeElement.getQualifiedName().toString();
        }
    }

    // 输出提示信息
    private void printNote(String msg, Object... args) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, String.format(msg, args));
    }

    // 获取两个包名字符串公共的包名
    private String getPublicPackageName(String pkgName1, String pkgName2) {
        if (Strings.isNullOrEmpty(pkgName1) && Strings.isNullOrEmpty(pkgName2)) {
            return UUID.randomUUID().toString()
                    .replaceAll("[^0-9a-zA-Z]+", "")
                    .toUpperCase();
        }
        if (Strings.isNullOrEmpty(pkgName1)) {
            return pkgName2;
        } else if (Strings.isNullOrEmpty(pkgName2)) {
            return pkgName1;
        }

        StringBuilder sb = new StringBuilder();
        String[] split1 = pkgName1.split("\\.");
        String[] split2 = pkgName2.split("\\.");
        int minLen = split1.length;
        if (split1.length > split2.length) {
            minLen = split2.length;
        }

        for (int i = 0; i < minLen; i++) {
            if (split1[i].equals(split2[i])) {
                sb.append(split1[i]).append(".");
            }
        }

        if (sb.length() == 0 || !split1[0].equals(split2[0])) {
            return pkgName1;
        }

        String pkgName = sb.toString();
        if (pkgName.endsWith(".")) {
            return pkgName.substring(0, pkgName.length() - 1);
        }
        return pkgName;
    }
}
