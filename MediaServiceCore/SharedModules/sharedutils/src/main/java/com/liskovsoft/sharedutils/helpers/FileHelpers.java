package com.liskovsoft.sharedutils.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.liskovsoft.sharedutils.mylogger.Log;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FileHelpers {
    private static final String TAG = FileHelpers.class.getSimpleName();

    public static File getDownloadDir(Context context) {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

     
     
     
     
     
     
     
     
     
     
     

    public static File getCacheDir(Context context) {
        File cacheDir = getInternalCacheDir(context);

        if (cacheDir == null) {
            cacheDir = getExternalCacheDir(context);
        }

        return cacheDir;
    }

    public static File getInternalCacheDir(Context context) {
        if (context == null) {
            return null;
        }

        return context.getCacheDir();
    }

    public static File getExternalCacheDir(Context context) {
        if (context == null) {
            return null;
        }

        File cacheDir = context.getExternalCacheDir();

        if (cacheDir == null || !cacheDir.canWrite()) {
             
            cacheDir = getExternalStorageDirectory("cache");
        }

        return cacheDir;
    }

    public static File getExternalFilesDir(Context context) {
        if (context == null) {
            return null;
        }

        File filesDir = context.getExternalFilesDir(null);

        if (filesDir == null || !filesDir.canWrite()) {
             
            filesDir = getExternalStorageDirectory("files");
        }

        return filesDir;
    }

    private static File getExternalStorageDirectory(String subdir) {
        if (TextUtils.isEmpty(subdir)) {
            return null;
        }

        File rootDir = Environment.getExternalStorageDirectory();

        if (rootDir == null || !rootDir.canWrite()) {
            return null;
        }

        File storagePath = new File(rootDir, subdir);

        if (!storagePath.exists()) {
            storagePath.mkdirs();
        }

        return storagePath;
    }

     
    public static void checkCachePermissions(Context context) {
        File cacheDir = null;

        try {
             
            cacheDir = context.getExternalCacheDir();
        } catch (ArrayIndexOutOfBoundsException e) {  
            e.printStackTrace();
        }

        if (cacheDir == null || !cacheDir.canWrite()) {  
            cacheDir = Environment.getExternalStorageDirectory();

            if (cacheDir == null || !cacheDir.canWrite()) {
                if (VERSION.SDK_INT <= 23) {
                     
                    PermissionHelpers.verifyStoragePermissions(context);  
                }
            }
        }
    }

    public static File getBackupDir(Context context) {
        return new File(Environment.getExternalStorageDirectory(), String.format("data/%s", context.getPackageName()));
    }

     
    public static boolean isEmpty(File dir) {
        return dir == null || listFileTree(dir).size() == 0;
    }

     
    public static Collection<File> listFileTree(File dir) {
        Set<File> fileTree = new HashSet<>();

        if (dir == null || dir.listFiles() == null){
            return fileTree;
        }

        for (File entry : dir.listFiles()) {
            if (entry.isFile()) {
                fileTree.add(entry);
            } else {
                fileTree.addAll(listFileTree(entry));
            }
        }

        return fileTree;
    }

     
    public static void deleteCache(Context context) {
        deleteContent(getInternalCacheDir(context));
        deleteContent(getExternalCacheDir(context));
    }

    public static boolean delete(String filePath) {
        return filePath != null && delete(new File(filePath));
    }

    public static boolean delete(File sourceLocation) {
        return deleteRecursive(sourceLocation, true);
    }

    public static boolean deleteContent(File sourceLocation) {
        return deleteRecursive(sourceLocation, false);
    }

    private static boolean deleteRecursive(File sourceLocation, boolean deleteRoot) {
        return deleteRecursive(sourceLocation, deleteRoot, 0);
    }

     
    private static boolean deleteRecursive(File sourceLocation, boolean deleteRoot, int level) {
        if (sourceLocation != null && sourceLocation.isDirectory()) {
            String[] children = sourceLocation.list();
            if (children != null && level < 10) {  
                for (String child : children) {
                    boolean success = deleteRecursive(new File(sourceLocation, child), true, level + 1);
                    if (!success) {
                        return false;
                    }
                }
            }
            return deleteRoot ? sourceLocation.delete() : true;
        } else if (sourceLocation != null && sourceLocation.isFile()) {
            return sourceLocation.delete();
        } else {
            return false;
        }
    }

    public static void deleteByPrefix(File directory, String prefix) {
        if (directory == null) {
            return;
        }

        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file != null && file.isDirectory()) {
                deleteByPrefix(file, prefix);
            } else if (file != null && file.isFile() && file.getName().startsWith(prefix)) {
                file.delete();
            }
        }
    }

    public static void copy(File sourceLocation, File targetLocation) {
        if (sourceLocation.isDirectory()) {
            copyDirectory(sourceLocation, targetLocation);
        } else {
            try {
                copyFile(sourceLocation, targetLocation);
            } catch (IOException e) {
                Log.e(TAG, "Unable to copy: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void copy(InputStream source, File destination) {
        if (destination.getParentFile() != null && !destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs();
        }

        try {
            IOUtils.copy(source, new FileOutputStream(destination));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyDirectory(File source, File target) {
        if (!target.exists()) {
            target.mkdirs();
        }

        String[] list = source.list();

        if (list == null) {
            Log.w(TAG, "Seems that read permissions not granted for file: " + source.getAbsolutePath());
            return;
        }

        for (String f : list) {
            copy(new File(source, f), new File(target, f));
        }
    }

    private static void copyFile(File source, File target) throws IOException {
        try (
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target)
        ) {
            byte[] buf = new byte[1024];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        }
    }

    public static void streamToFile(InputStream is, File destination) {
        if (is == null || destination == null) {
            return;
        }

        FileOutputStream fos = null;

        try {
            if (destination.getParentFile() != null) {
                destination.getParentFile().mkdirs();  
            }
            destination.createNewFile();  

            fos = new FileOutputStream(destination);

            byte[] buffer = new byte[1024];
            int len1;
            while ((len1 = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len1);
            }
        } catch (FileNotFoundException ex) {
            Log.e(TAG, "Open file failed: Seemed EACCES (Permission denied): %s", ex.getMessage());
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        } finally {
            closeStream(fos);
            closeStream(is);
        }
    }

    public static void stringToFile(String is, File destination) {
        streamToFile(toStream(is), destination);
    }

    public static String toString(InputStream in) {
        try {
            int bufsize = 8196;
            char[] cbuf = new char[bufsize];
            StringBuilder buf = new StringBuilder(bufsize);
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");

            int readBytes;
            while ((readBytes = reader.read(cbuf, 0, bufsize)) != -1) {
                buf.append(cbuf, 0, readBytes);
            }

            return buf.toString();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        return null;
    }

     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     

     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     

     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     

     
     
     
     
     
     
     
     
     
     
     
     

    public static InputStream toStream(String content) {
        if (content == null) {
            return null;
        }

        return new ByteArrayInputStream(content.getBytes(Charset.forName("UTF8")));
    }

    public static void closeStream(Closeable fos) {
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     
    @Nullable
    public static Uri getFileUri(Context context, String filePath) {
         
         
        if (VERSION.SDK_INT >= 24) {
            try {
                return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".update_provider", new File(filePath));
            } catch (IllegalArgumentException e) {
                 
                return null;
            }
        } else {
            return Uri.fromFile(setReadable(new File(filePath)));
        }
    }

    @SuppressLint("SetWorldReadable")
    private static File setReadable(File file) {
         
         
        file.setReadable(true, false);
        return file;
    }

    public static Uri getFileUri(Context context, File filePath) {
        if (filePath == null) {
            return null;
        }

        return getFileUri(context, filePath.getAbsolutePath());
    }

    public static InputStream appendStream(InputStream first, InputStream second) {
        if (first == null && second == null) {
            return null;
        }

        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return new SequenceInputStream(first, second);
    }

     
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

     
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return isExternalStorageWritable() || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static boolean isFileExists(String path) {
        if (path == null) {
            return false;
        }

        return new File(path).exists();
    }

    public static boolean isFileExists(File path) {
        if (path == null) {
            return false;
        }

        return path.exists();
    }

    public static void ensureFileExists(File file) {
        if (file == null) {
            return;
        }

        try {
            if (!file.exists()) {
                if (file.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                }

                file.createNewFile();
            }
        } catch (IOException e) {
            Log.d(TAG, "ensureFileExists: " + e.getMessage());
            e.printStackTrace();
        }
    }

     
    public static boolean isFreshFile(String path, int freshTimeMS) {
        if (path == null) {
            return false;
        }

        File file = new File(path);

        if (!file.exists()) {
            return false;
        }

        int fileSizeKB = Integer.parseInt(String.valueOf(file.length() / 1024));

        if (fileSizeKB < 1_000) {  
            return false;
        }

        return System.currentTimeMillis() - file.lastModified() < freshTimeMS;
    }

    public static String getFileContents(File source) {
        if (source == null) {
            return null;
        }

        String result = null;
        try {
            result = toString(new FileInputStream(source));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: %s", source.getAbsolutePath());
        }
        return result;
    }

     
    public static long getDirSize(File dir) {
        if (dir == null) {
            return 0;
        }

        File[] files = dir.listFiles();

        if (files == null) {
            return 0;
        }

        long size = 0;
        for (File file : files) {
            if (file != null && file.isDirectory()) {
                size += getDirSize(file);
            } else if (file != null && file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }
}
