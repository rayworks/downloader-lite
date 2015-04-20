
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

package org.rayworks.network.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by seanzhou on 12/25/14.
 *
 * Code taken from com.nostra13.universalimageloader.utils.IoUtils
 */
public final class IOUtils {
    //public static final int DEFAULT_IMAGE_TOTAL_SIZE = 1024;
    //public static final int CONTINUE_LOADING_PERCENTAGE = 75;

    /** Listener and controller for copy process */
    public static interface CopyListener {

        /**
         * @param current Loaded bytes
         * @param total   Total bytes for loading
         * @return <b>true</b> - if copying should be continued; <b>false</b> - if copying should be interrupted
         */
        boolean onBytesCopied(int current, int total);
    }


    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Copies stream, fires progress events by listener, can be interrupted by listener.
     *
     * @param is         Input stream
     * @param os         Output stream
     * @param listener   null-ok; Listener of copying progress and controller of copying interrupting
     * @param bufferSize Buffer size for copying, also represents a step for firing progress listener callback, i.e.
     *                   progress event will be fired after every copied <b>bufferSize</b> bytes
     * @return <b>true</b> - if stream copied successfully; <b>false</b> - if copying was interrupted by listener
     * @throws java.io.IOException
     */
    public static boolean copyStream(InputStream is, OutputStream os, CopyListener listener, int bufferSize, int totalSize)
            throws IOException {
        int current = 0;
        int total = totalSize;

        final byte[] bytes = new byte[bufferSize];
        int count;
        if (shouldStopLoading(listener, current, total)) return false;
        while ((count = is.read(bytes, 0, bufferSize)) != -1) {
            os.write(bytes, 0, count);
            current += count;
            if (shouldStopLoading(listener, current, total)) return false;
        }
        os.flush();
        return true;
    }

    private static boolean shouldStopLoading(CopyListener listener, int current, int total) {
        if (listener != null) {
            boolean shouldContinue = listener.onBytesCopied(current, total);
            return !shouldContinue;
        }
        return false;
    }
}
