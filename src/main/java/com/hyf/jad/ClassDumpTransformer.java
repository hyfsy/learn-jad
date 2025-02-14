package com.hyf.jad;


import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author beiwei30 on 25/11/2016.
 */
public class ClassDumpTransformer implements ClassFileTransformer {

    private Set<Class<?>>       classesToEnhance;
    private Map<Class<?>, File> dumpResult;
    private File                arthasLogHome;

    private File directory;

    public ClassDumpTransformer(Set<Class<?>> classesToEnhance, File directory) {
        this.classesToEnhance = classesToEnhance;
        this.dumpResult = new HashMap<Class<?>, File>();
        this.directory = directory;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        try {
            if (classesToEnhance.contains(classBeingRedefined)) {
                dumpClassIfNecessary(classBeingRedefined, classfileBuffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<Class<?>, File> getDumpResult() {
        return dumpResult;
    }

    private void dumpClassIfNecessary(Class<?> clazz, byte[] data) {
        String className = clazz.getName();
        ClassLoader classLoader = clazz.getClassLoader();
        String classDumpDir = "classdump";

        // 创建类所在的包路径
        if (!directory.mkdirs() && !directory.exists()) {
            return;
        }

        String fileName;
        if (classLoader != null) {
            fileName = classLoader.getClass().getName() + "-" + Integer.toHexString(classLoader.hashCode()) +
                    File.separator + className.replace(".", File.separator) + ".class";
        }
        else {
            fileName = className.replace(".", File.separator) + ".class";
        }

        File dumpClassFile = new File(directory, fileName);

        // 将类字节码写入文件
        try {
            FileUtils.writeByteArrayToFile(dumpClassFile, data, false);
            dumpResult.put(clazz, dumpClassFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
