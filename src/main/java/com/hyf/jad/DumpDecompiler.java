package com.hyf.jad;

import com.hyf.hotrefresh.core.util.Util;
import sun.tools.jar.resources.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author baB_hyf
 * @date 2022/01/23
 */
public class DumpDecompiler {

    public static void main(String[] args) {

        // String searchClass = "com.hyf.*";
        // String saveDirectory = "C:\\Users\\baB_hyf\\Desktop\\test";
        //
        // decompileClass(searchClass, saveDirectory);


        // String searchJar = "E:\\study\\idea4\\project\\learn-jad\\lib\\cfr-0.151.jar";
        String saveDirectory2 = "C:\\Users\\baB_hyf\\Desktop\\test";



        // String searchJar = "C:\\Users\\baB_hyf\\Desktop\\hotcode2\\lib\\hotcode2.autoremote.jar";
        // decompileJar(searchJar, saveDirectory2, "com.taobao");



        long start = System.currentTimeMillis();
        String searchFilePath = "C:\\Users\\baB_hyf\\Desktop\\hotcode2\\";
        decompilePath(searchFilePath, saveDirectory2, "");
        System.out.println(System.currentTimeMillis() - start);
    }

    public static void decompilePath(String filePath, String savePath, String placeHolder) {
        File file = new File(filePath);

        URL url = null;
        try {
            url = new File(filePath).toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLClassLoader ucl = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());

        Set<Class<?>> allClasses = decompilePath(ucl, file, file, savePath);

        decompile(savePath, allClasses);
    }

    public static Set<Class<?>> decompilePath(ClassLoader cl, File root, File file, String savePath) {

        Set<Class<?>> allClasses = new HashSet<Class<?>>();

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                return allClasses;
            }
            for (File f : files) {
                allClasses.addAll(decompilePath(cl, root, f, savePath));
            }
        }
        else {
            if (file.getName().endsWith(".class")) {
                String className = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1).replace(File.separator, ".");
                className = className.substring(0, className.length() - 6);
                try {
                    Class<?> clazz = cl.loadClass(className);
                    allClasses.add(clazz);
                } catch (ClassNotFoundException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        return allClasses;
    }

    public static void decompileJar(String jarPath, String savePath, String startWith) {

        if (!savePath.endsWith(File.separator)) {
            savePath = savePath + File.separator;
        }

        try{
            URL url = new File(jarPath).toURI().toURL();
            URLClassLoader ucl = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());

            Set<Class<?>> allClasses = new HashSet<Class<?>>();


            JarFile jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            JarEntry entry;
            while(entries.hasMoreElements()){
                entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entryName.endsWith(".class")) {
                    continue;
                }

                String className = entryName.substring(0, entryName.length() - 6).replace("/", ".");
                if (!className.startsWith(startWith)) {
                    continue;
                }

                File file = new File(savePath + entryName.replace("/", File.separator));

                InputStream is = jarFile.getInputStream(entry);
                FileUtils.writeInputStreamToFile(file, is, false);

                try {
                    allClasses.add(ucl.loadClass(className));
                } catch (Throwable e) {
                    System.out.println(e.getMessage());
                }
            }

            decompile(savePath, allClasses);

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void decompileClass(String searchClass, String savePath) {

        Instrumentation instrumentation = getInstrumentation();

        Set<Class<?>> allClasses = new HashSet<Class<?>>();
        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(instrumentation, searchClass, false);
        for (Class<?> matchedClass : matchedClasses) {
            Set<Class<?>> withInnerClasses = SearchUtils.searchClassOnly(instrumentation, matchedClasses.iterator().next().getName() + "$*", false, null);
            allClasses.addAll(withInnerClasses);
            allClasses.add(matchedClass);
        }
        Iterator<Class<?>> it = allClasses.iterator();
        while (it.hasNext()) {
            Class<?> clazz = it.next();
            if (DumpDecompiler.isLambdaClass(clazz)) {
                it.remove();
            }
        }
        // allClasses.removeIf(DumpDecompiler::isLambdaClass);

        decompile(savePath, allClasses);
    }

    public static void decompile(String savePath, Set<Class<?>> allClasses) {

        Instrumentation instrumentation = getInstrumentation();

        ClassDumpTransformer classDumpTransformer = new ClassDumpTransformer(allClasses, new File(savePath));
        instrumentation.addTransformer(classDumpTransformer, true);

        if (allClasses.isEmpty()) {
            return;
        }

        Class<?>[] classes = allClasses.toArray(new Class<?>[0]);
        for (int i = 0; i < allClasses.size(); i++) {
            try {
                instrumentation.retransformClasses(classes[i]);
            } catch (UnmodifiableClassException e) {
                e.printStackTrace();
            }
            catch (Throwable t) {
                System.out.println(classes[i].getName());
            }
            // finally {
            //     instrumentation.removeTransformer(classDumpTransformer);
            // }
        }

        Map<Class<?>, File> dumpResult = classDumpTransformer.getDumpResult();

        for (Map.Entry<Class<?>, File> entry : dumpResult.entrySet()) {
            Class<?> c = entry.getKey();
            File f = entry.getValue();

            String data = Decompiler.decompile(f.getAbsolutePath());
            try {
                File saveFile = new File(f.getParentFile(), f.getName().substring(0, f.getName().lastIndexOf(".")) + ".java");
                FileUtils.writeStringToFile(saveFile, data, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Instrumentation instrumentation;

    public static Instrumentation getInstrumentation() {
        if (instrumentation == null) {
            instrumentation = Util.getInstrumentation();
        }
        return instrumentation;
    }

    public static boolean isLambdaClass(Class<?> clazz) {
        return clazz.getName().contains("$$Lambda$");
    }
}
