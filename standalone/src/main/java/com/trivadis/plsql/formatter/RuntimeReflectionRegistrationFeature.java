package com.trivadis.plsql.formatter;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

@SuppressWarnings("unused")
public class RuntimeReflectionRegistrationFeature implements Feature {
    private static final String[] SKIP_CLASS_NAMES = {
            "oracle.dbtools.util.Closeables",
    };

    private static void register(String packageName, boolean includeSubPackages, ClassLoader classLoader) {
        Reflections reflections = new Reflections(packageName);
        Set<String> allClassNames = reflections.getAll(Scanners.SubTypes);
        // allClassNames contains also inner classes
        for (String className : allClassNames) {
            if (className.startsWith((packageName))) {
                if (includeSubPackages || (!className.substring(packageName.length() + 1).contains("."))) {
                    if (validClass(className)) {
                        registerClass(className, classLoader);
                    }
                }
            }
        }
    }

    private static boolean validClass(String className) {
        for (String skipClassName : SKIP_CLASS_NAMES) {
            if (className.startsWith(skipClassName)) {
                return false;
            }
        }
        return true;
    }

    private static void registerClass(String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            // calling getClass() on a clazz throws an Exception when not found on the classpath
            RuntimeReflection.register(clazz.getClass());
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                RuntimeReflection.register(constructor);
            }
            for (Method method : clazz.getDeclaredMethods()) {
                RuntimeReflection.register((method));
            }
            for (Field field : clazz.getDeclaredFields()) {
                RuntimeReflection.register(field);
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    public void beforeAnalysis(BeforeAnalysisAccess access) {
        ClassLoader classLoader = access.getApplicationClassLoader();
        // register all classes in a package
        register("oracle.dbtools.app", true, classLoader);
        register("oracle.dbtools.arbori", true, classLoader);
        register("oracle.dbtools.parser", true, classLoader);
        register("oracle.dbtools.raptor", false, classLoader);
        register("oracle.dbtools.util", true, classLoader);
    }
}
