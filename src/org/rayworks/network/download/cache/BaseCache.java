
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.rayworks.network.util.IOUtils;

/**
 * Created by seanzhou on 12/25/14.
 * Cache interface defines the operations for managing download stuffs.
 */
public interface BaseCache {
    File getCacheDir();

    /**
     * Returns file of cached resource
     *
     * @param fileUri Original resource URI
     * @return File of cached resource or <b>null</b> if resource wasn't cached
     */
    File getFile(String fileUri);

    boolean existFile(String fileUri);


    /***
     * Gets partial downloaded file
     * @param fileUri
     * @return
     */
    public File getTempFile(String fileUri);

    /**
     * Saves file stream in disk cache.
     * Incoming stream shouldn't be closed in this method.
     *
     * @param remoteFileUri Original file URI
     * @param inputStream   Input stream of file (shouldn't be closed in this method)
     * @param totalLength   Total length of remote file
     * @param listener      Listener for saving progress, can be ignored if you don't use
     *                      {@linkplain org.rayworks.network.util.IOUtils.CopyListener} in this calls
     * @return <b>true</b> - if file was saved successfully; <b>false</b> - if file wasn't saved in the cache.
     * @throws java.io.IOException
     */
    boolean save(String remoteFileUri, InputStream inputStream, int totalLength, IOUtils.CopyListener listener) throws IOException;

    /**
     * Removes specified cache by key
     *
     * @param key
     */
    void removeByKey(String key);

    /**
     * Cleans up all the caches
     */
    void clearAll();
}
