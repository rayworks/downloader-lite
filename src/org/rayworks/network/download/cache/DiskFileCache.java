
/*
 * Copyright (c) 2015 rayworks
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rayworks.network.download.cache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.PriorityQueue;
import java.util.concurrent.Executor;

import org.rayworks.network.util.EFLogger;
import org.rayworks.network.util.FileNameGenerator;
import org.rayworks.network.util.IOUtils;
import org.rayworks.network.util.Md5FileNameGenerator;

/**
 * Created by seanzhou on 12/25/14.
 */
public class DiskFileCache implements BaseCache {
    public static final String TAG = DiskFileCache.class.getSimpleName();

    private File root;
    private long totalCachedSize = 0;
    public static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 32 Kb
    public static final String TEMP_FILE_POSTFIX = ".download";

    private final Executor executor;
    private Object lock = new Object();
    private boolean isTrimPending;
    private boolean isTrimInProgress;

    private Limits limits;

    interface CacheRemovalListener{
        void onRemoveStarted();
        void onRemoveComplete();
    }

    private FileNameGenerator fileNameGenerator = new Md5FileNameGenerator();
    /***
     * Constructor
     * @param rootDir   cache root directory
     * @param executor  Executor for background operation
     * @param limits    limitation for current cache
     */
    public DiskFileCache(File rootDir, Executor executor, Limits limits) {
        root = rootDir;
        this.executor = executor;

        this.limits = limits;
    }

    /***
     * Constructor with default limitation
     * @param rootDir   cache root directory
     * @param executor  Executor for background operation
     */
    public DiskFileCache(File rootDir, Executor executor) {
        this(rootDir, executor, new Limits());
    }

    @Override
    public File getCacheDir() {
        return root;
    }

    @Override
    public File getFile(String fileUri) {
        return getCacheFile(fileUri);
    }

    @Override
    public boolean existFile(String fileUri) {
        return getCacheFile(fileUri).exists();
    }

    @Override
    public File getTempFile(String fileUri){
        File file = getCacheFile(fileUri);
        return new File(file.getAbsoluteFile() + TEMP_FILE_POSTFIX);
    }

    @Override
    public boolean save(String remoteFileUri, InputStream inputStream, int totalLength, IOUtils.CopyListener listener) throws IOException {
        File cacheFile = getCacheFile(remoteFileUri);
        File tmpFile = new File(cacheFile.getAbsoluteFile() + TEMP_FILE_POSTFIX);

        int total = 0;
        boolean loaded;
        boolean readingMode = true;
        try {
            OutputStream fos = new BufferedOutputStream(new FileOutputStream(tmpFile, true), DEFAULT_BUFFER_SIZE); // appendable
            byte[] buf = new byte[DEFAULT_BUFFER_SIZE];

            int readCnt = 0;

            try {
                total = totalLength; //inputStream.available();// not reliable?!
                while (readingMode && (readCnt = inputStream.read(buf)) != -1) {
                    fos.write(buf, 0, readCnt);
                    fos.flush();

                    if (listener != null) { // to calculate the total progress, including the existed part
                        readingMode = listener.onBytesCopied((int) tmpFile.length(), total);
                    }
                }
            }finally {
                IOUtils.closeSilently(fos);
            }

        }finally {

            loaded = total == tmpFile.length();
            if(readingMode && loaded){
                if(!tmpFile.renameTo(cacheFile)){
                    loaded = false;
                    tmpFile.setLastModified(System.currentTimeMillis());
                }else{
                    cacheFile.setLastModified(System.currentTimeMillis());
                    EFLogger.d("", "file renamed successful dest file# " + cacheFile.getName());

                    // The following logic modified from com.facebook.internal.FileLruCache
                    // TODO: the recursive delete action needs to be considered

                    // However, it does not need to be synchronized, since in the race we will just start an unnecessary trim
                    // operation.  Avoiding the cost of holding the lock across the file operation seems worth this cost.
                    postTrim();
                }
            }
        }

        return loaded;
    }

    private File getCacheFile(String remoteFileUri) { // get the simple name of the remote file
        String realName = fileNameGenerator.generate(remoteFileUri); //remoteFileUri.substring(remoteFileUri.lastIndexOf("/") + 1);
        return new File(root, realName);
    }

    @Override
    public void removeByKey(String key) {
        File file = getCacheFile(key);
        if(file.exists()){
            file.delete();
        }

        File tmp = getTempFile(key);
        if(tmp.exists()){
            tmp.delete();
        }
    }

    @Override
    public void clearAll() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                File[] files = root.listFiles();
                if (files != null) {
                    for (File f : files) {
                        f.delete();
                    }
                }
            }
        });
    }

    /***
     * The following code about cache management was taken from
     * https://github.com/facebook/facebook-android-sdk/blob/master/facebook/src/com/facebook/internal/FileLruCache.java
     */
    private void postTrim() {
        synchronized (lock) {
            if (!isTrimPending) {
                isTrimPending = true;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        trim();
                    }
                });
            }
        }
    }

    private void trim() {
        synchronized (lock) {
            isTrimPending = false;
            isTrimInProgress = true;
        }
        try {
            EFLogger.d(TAG, "trim started");
            PriorityQueue<ModifiedFile> heap = new PriorityQueue<ModifiedFile>();
            long size = 0;
            long count = 0;
            File[] filesToTrim = this.root.listFiles();
            if (filesToTrim != null) {
                for (File file : filesToTrim) {
                    ModifiedFile modified = new ModifiedFile(file);
                    heap.add(modified);
                    EFLogger.d(TAG, "  trim considering time=" + Long.valueOf(modified.getModified())
                            + " name=" + modified.getFile().getName());

                    size += file.length();
                    count++;
                }
            }

            while ((size > limits.getByteCount()) || (count > limits.getFileCount())) {
                File file = heap.remove().getFile();
                EFLogger.d(TAG, "  trim removing " + file.getName());
                size -= file.length();
                count--;
                file.delete();
                EFLogger.d(TAG, "  after removing cache size:" + size);
            }
        } finally {
            synchronized (lock) {
                isTrimInProgress = false;
                lock.notifyAll();
            }
        }
    }

    public static final class Limits {
        private int byteCount;
        private int fileCount;

        public Limits() {
            // A Samsung Galaxy Nexus can create 1k files in half a second.  By the time
            // it gets to 5k files it takes 5 seconds.  10k files took 15 seconds.  This
            // continues to slow down as files are added.  This assumes all files are in
            // a single directory.
            //
            // Following a git-like strategy where we partition MD5-named files based on
            // the first 2 characters is slower across the board.
            this.fileCount = 1024;
            this.byteCount = 32 * 1024 * 1024; // 32MB
        }

        /**
         * Limitation for cache setting
         *
         * @param byteLimitCnt
         * @param sizeLimitCnt file count limitation; if it's 0, the value will be MAX_INT.
         */
        public Limits(int byteLimitCnt, int sizeLimitCnt) {
            setByteCount(byteLimitCnt);
            setFileCount(sizeLimitCnt);
        }

        int getByteCount() {
            return byteCount;
        }

        int getFileCount() {
            return fileCount;
        }

        void setByteCount(int n) {
            if (n < 0) {
                throw new InvalidParameterException("Cache byte-count limit must be >= 0");
            }
            byteCount = n;
        }

        void setFileCount(int n) {
            if (n < 0) {
                throw new InvalidParameterException("Cache file count limit must be >= 0");
            }
            if(n == 0){
                n = Integer.MAX_VALUE;
            }
            fileCount = n;
        }
    }

    // Caches the result of lastModified during sort/heap operations
    private final static class ModifiedFile implements Comparable<ModifiedFile> {
        private static final int HASH_SEED = 29; // Some random prime number
        private static final int HASH_MULTIPLIER = 37; // Some random prime number

        private final File file;
        private final long modified;

        ModifiedFile(File file) {
            this.file = file;
            this.modified = file.lastModified();
        }

        File getFile() {
            return file;
        }

        long getModified() {
            return modified;
        }

        @Override
        public int compareTo(ModifiedFile another) {
            if (getModified() < another.getModified()) {
                return -1;
            } else if (getModified() > another.getModified()) {
                return 1;
            } else {
                return getFile().compareTo(another.getFile());
            }
        }

        @Override
        public boolean equals(Object another) {
            return
                    (another instanceof ModifiedFile) &&
                            (compareTo((ModifiedFile) another) == 0);
        }

        @Override
        public int hashCode() {
            int result = HASH_SEED;

            result = (result * HASH_MULTIPLIER) + file.hashCode();
            result = (result * HASH_MULTIPLIER) + (int) (modified % Integer.MAX_VALUE);

            return result;
        }
    }
}
