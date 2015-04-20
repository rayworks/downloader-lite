
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

package org.rayworks.network.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rayworks.network.download.listener.DownloadListener;

public interface DownloadService {
    /**
     * Sets the configuration
     */
    void config(DownloadSetting setting);

    /**
     * Whether a batch of remote files have already been downloaded
     *
     * @param urls
     * @return
     */
    boolean haveAllFilesDownloaded(List<String> urls);

    /**
     * Retrieves the specified cached file by its url
     *
     * @param remoteUrl
     * @return a file if cache hits, otherwise null.
     */
    File getCachedFileByUrl(String remoteUrl);

    /**
     * Adds a simple task
     *
     * @param url
     * @param downloadListener
     */
    void add(String url, DownloadListener downloadListener);

    /**
     * Adds a batch of unrelated tasks which will keep their own progress
     *
     * @param requests
     */
    void add(ArrayList<DownloadRequest> requests);

    /**
     * Adds a composite task who contains sub-tasks and holds a unified progress
     *
     * @param urls
     * @param downloadListener
     */
    void addBatchedTask(List<String> urls, DownloadListener downloadListener);

    /**
     * Cancels all the tasks
     */
    void cancelAllTasks();

    /**
     * Cancels one or many tasks which meets the condition.
     *
     * @param urlTag
     */
    void cancelTask(String urlTag);

    /**
     * Promotes the new task's priority
     *
     * @param url
     * @param downloadListener
     */
    void prioritizeNewTask(String url, DownloadListener downloadListener);

    //void prioritizeCurrentTasks()
    // ScheduleStrategy: FIFO LRU TARGED_FIRST
    //void scheduleAllTask();
}
