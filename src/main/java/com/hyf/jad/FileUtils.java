package com.hyf.jad;

import java.io.*;

/**
 * @author baB_hyf
 * @date 2022/01/23
 */
public class FileUtils {

    public static void writeInputStreamToFile(File file, InputStream data, boolean append) throws IOException {
        OutputStream out = null;
        InputStream is = null;
        try {
            out = new BufferedOutputStream(openOutputStream(file, append));
            is = new BufferedInputStream(data);
            int len;
            byte[] bytes = new byte[1024];
            while ((len = is.read(bytes)) > 0) {
                out.write(bytes, 0, len);
            }
            is.close();
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static void writeStringToFile(File file, String data, boolean append) throws IOException {
        Writer out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(openOutputStream(file, append)));
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static void writeByteArrayToFile(File file, byte[] data, boolean append) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(openOutputStream(file, append));
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (!file.canWrite()) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        }
        else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }
}
