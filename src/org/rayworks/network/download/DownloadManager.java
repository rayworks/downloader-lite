
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.rayworks.network.download.cache.BaseCache;
import org.rayworks.network.download.listener.DownloadListener;
import org.rayworks.network.download.listener.WrapperDownloadListener;
import org.rayworks.network.storage.SyncStateStore;
import org.rayworks.network.util.EFLogger;
import org.rayworks.service.ConnectivityService;
import org.rayworks.service.ConnectivityStateEvent;
import org.rayworks.service.DeviceStorageMonitor;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * <ul>
 * A download component which is able to:
 * </ul>
 *
 * <li>download multiply resources asynchronously</li>
 * <li>download a compound task which contains subtasks, holding an unified progress</li>
 * <li>pause tasks</li>
 * <li>prioritize tasks for downloading</li>
 * <li>continue downloading file from break point</li>
 * <li>manage download caches according to setting</li>
 */
public class DownloadManager implements DownloadService, ConnectivityService.ConnectivityStateEventListener {
    private static final String TAG = DownloadManager.class.getSimpleName();

    private DownloadSetting downloadSetting;
    
    private final SyncStateStore syncStateStore;

    private final int workThreadNum;

    private final LinkedBlockingDeque<BackgroundTask> ongoingTasks;

    private WorkerThread[] workThreads;

    private final ConnectivityService connectivityService;
    private final DeviceStorageMonitor deviceStorageMonitor;

    private final BaseCache cache;
    private DownloadEnabledStrategy downloadEnabledStrategy;

    public BaseCache getDownloadCache() {
        return cache;
    }
    
    public ArrayList<BackgroundTask> recoverableTasks = new ArrayList<>();

    /**
     * Constructor
     *
     * @param syncStateStore       storage for state of {@link SyncEntity}, e.g, Last-Modified
     * @param setting              configuration for downloading
     * @param connectivityService  service to detect network changed events
     * @param deviceStorageMonitor monitor to check whether the device storage is available for downloading
     * @param baseCache            cache for download
     */
    public DownloadManager(SyncStateStore syncStateStore,
                           DownloadSetting setting,
                           ConnectivityService connectivityService,
                           DeviceStorageMonitor deviceStorageMonitor,
                           BaseCache baseCache) {
        checkNotNull(syncStateStore);
        checkNotNull(setting);
        checkNotNull(connectivityService);
        checkNotNull(deviceStorageMonitor);
        checkNotNull(baseCache);
        
        downloadSetting = setting;
        this.syncStateStore = syncStateStore;
        this.workThreadNum = setting.getThreadNum();

        ongoingTasks = new LinkedBlockingDeque<>(); //new LinkedBlockingQueue<>();

        workThreads = new WorkerThread[workThreadNum];

        this.connectivityService = connectivityService;
        this.deviceStorageMonitor = deviceStorageMonitor;

        this.cache = baseCache;

        this.connectivityService.addListener(this);

        downloadEnabledStrategy = setting.getDownloadEnabledStrategy();
        checkNotNull(downloadEnabledStrategy);

        restoreSyncTasks();
        
        start();
    }

    private void restoreSyncTasks() {
        /*TO DO*/
    }
    
    private void start() {
        WorkerThread workThread;
        for (int i = 0; i < workThreadNum; i++) {
            workThread = new WorkerThread("Thread#" + i, ongoingTasks, syncStateStore, cache);
            workThreads[i] = workThread;

            workThread.start();
        }
    }

    @Override
    public void cancelAllTasks() {
        /*for (int i = 0; i < workThreadNum; i++) {
            WorkerThread thread = workThreads[i];
            thread.stopNow();
        }*/
        
        restWorkers();
    }

    @Override
    public void config(DownloadSetting setting) {
        if(setting != null){
            downloadSetting = setting;
            downloadEnabledStrategy = downloadSetting.getDownloadEnabledStrategy();
            checkNotNull(downloadEnabledStrategy);
        }
    }

    @Override
    public boolean haveAllFilesDownloaded(List<String> urls) {
        boolean allDownloaded = false;
        for (String url : urls){
            allDownloaded = cache.existFile(url);
            if(!allDownloaded){
                return allDownloaded;
            }
        }
        return true;
    }

    @Override
    public File getCachedFileByUrl(String remoteUrl) {
        return cache.getFile(remoteUrl);
    }

    private void wakeupWorkers() {
        for (int i = 0; i < workThreadNum; i++) {
            WorkerThread thread = workThreads[i];
            thread.wakeup();
        }
    }

    private void restWorkers() {
        for (int i = 0; i < workThreadNum; i++) {
            WorkerThread thread = workThreads[i];
            thread.rest();
        }
    }

    private void rescheduleEnqueuedTasksAndResetThread(
            final WorkerThread workerThread,
            WorkerThread.TaskCancelledEventListener eventListener) {
        
        BackgroundTask backgroundTask = workerThread.getTask();
        if (backgroundTask != null) {
            recoverableTasks.add(backgroundTask.copyInstance());
            backgroundTask.releaseListener();
        }else {
            System.out.println(">>> Not found task in " + workerThread.getName());
        }
        workerThread.setTaskCancelledEventListener(eventListener);

        workerThread.rest();
    }

    /***
     * Checks whether it's a duplicate in-progress task
     *  
     * @param taskTag
     * @return
     */
    private boolean isTaskOngoing(final String taskTag){
        boolean ongoing = foundTaskInQueue(taskTag);
        if(!ongoing){
            ongoing = locateThreadWithTaskTag(taskTag) != null;
        }
        return ongoing;
    }

    /***
     * Whether a specified task is in the queue
     *
     * @param taskTag
     * @return
     */
    private boolean foundTaskInQueue(final String taskTag) {
        final Predicate<BackgroundTask> predicate = new Predicate<BackgroundTask>() {
            @Override
            public boolean apply(BackgroundTask backgroundTask) {
                return backgroundTask.containTargetKey(taskTag);
            }
        };

        boolean ongoing ;
        Collection<BackgroundTask> targetTasks = Collections2.filter(ongoingTasks, predicate);
        ongoing = targetTasks.size() > 0;
        return ongoing;
    }

    @Override
    public void cancelTask(String key) {
        boolean found = false;

        if(ongoingTasks.size() > 0) {
            found = cancelTaskInQueueWithTag(key);
        }

        if(!found) {
            cancelRunningTaskWithTag(key);
        }
        //EFLogger.d("", "Failed to cancel Task with key#" + key);

    }

    /***
     * Retrieves the thread which is executing  a specified task
     * 
     * @param tag
     * @return
     */
    private WorkerThread locateThreadWithTaskTag(String tag) {
        for (int i = 0; i < workThreadNum; i++) {
            WorkerThread thread = workThreads[i];
            final BackgroundTask backgroundTask = thread.getTask();
            if (backgroundTask != null && backgroundTask.containTargetKey(tag)) {
                EFLogger.d(TAG, "Worker located " + thread.getName() + " when running task #"+ tag);
                return thread;
            }
        }
        return null;
    }


    private void cancelRunningTaskWithTag(String tag) {
        WorkerThread thread = locateThreadWithTaskTag(tag);
        if(thread != null){

            // second check
            final BackgroundTask backgroundTask = thread.getTask();
            if (backgroundTask != null && backgroundTask.containTargetKey(tag)) {
                thread.cancelRunningTask();

                EFLogger.d("", "Task" + tag + " cancelled when running in the WorkerThread#" + thread.getName());
            }
        }
    }

/*    void pauseAllTasks(){
        restWorkers();
    }

    void resumeAllTasks(){
        wakeupWorkers();
    }*/

    @Override
    public void add(String url, DownloadListener downloadListener) {
        checkArgument(url != null);
        
        if(downloadEnabledStrategy.isNetworkAvailableForDownloading()) {
            if (!isTaskOngoing(url)) {
                ongoingTasks.add(new BackgroundTask(url, downloadListener));
            } else {
                EFLogger.d(TAG, "Ongoing task detected, request dumped now ...");
            }
        }else {
            downloadListener.onError("network not available for downloading");
        }
    }

    @Override
    public void add(ArrayList<DownloadRequest> requests) {
        checkNotNull(requests);
        
        for (DownloadRequest request : requests) {
            add(request.getUrl(), request.getDownloadListener());
        }
    }

    public void addBatchedTask(List<String> urls, DownloadListener downloadListener){
        checkNotNull(urls);
        checkNotNull(downloadEnabledStrategy);
        
        if(downloadEnabledStrategy.isNetworkAvailableForDownloading()) {
            if (urls.size() > 0) {
                String tag = urls.get(0);
                checkArgument(tag != null && !tag.equals(""));

                if (!isTaskOngoing(tag)) {
                    WrapperDownloadListener listener = new WrapperDownloadListener(downloadListener, urls);
                    ongoingTasks.add(new BackgroundTask(urls, listener));
                } else {
                    EFLogger.d(TAG, "Ongoing task detected, request dumped now ...");
                }
            }
        }else {
            downloadListener.onError("network not available for downloading");
        }
    }

    private boolean cancelTaskInQueueWithTag(final String url) {
        final Predicate<BackgroundTask> predicate = new Predicate<BackgroundTask>() {
            @Override
            public boolean apply(BackgroundTask backgroundTask) {
                return backgroundTask.containTargetKey(url);
            }
        };

        boolean found = false;
        Iterator<BackgroundTask> iterator = ongoingTasks.iterator();
        while (iterator.hasNext()){
            BackgroundTask bkgTask = iterator.next();

            if(predicate.apply(bkgTask)){
                iterator.remove();
                EFLogger.d(TAG, "Task" + url + " removed from queue");
                found = true;
                break;
            }
        }

        return found;
    }

    @Override
    public void prioritizeNewTask(final String url, final DownloadListener downloadListener) {
        if(isTaskOngoing(url)){
            EFLogger.d(TAG, "same task detected, prioritizing operation cancelled tag#" + url);
        }else {
            if(downloadEnabledStrategy.isNetworkAvailableForDownloading()){
                if(hasExtraWorker()){
                    // we have spare thread now, so just do it
                    ongoingTasks.addFirst(new BackgroundTask(url, downloadListener));
                    EFLogger.d(TAG, ">>> Having enough worker, just add the task at header, total task num:" + ongoingTasks.size());
                }else {
                    // adjust the queue
                    // TODO: find a longest running thread, interrupt it and reschedule the tasks
                    // switch the last thread to avoid interrupting the first one all the time
                    final WorkerThread workerThread = workThreads[workThreadNum - 1];
                    EFLogger.d(TAG, ">>>||| Reschedule the thread :" + workerThread.getName());

                    rescheduleEnqueuedTasksAndResetThread(workerThread, new WorkerThread.TaskCancelledEventListener() {
                        @Override
                        public void onTaskCancelled() {
                            EFLogger.d(TAG, ">>>||| interrupted tasks, task num:" + recoverableTasks.size());
                            
                            Iterator<BackgroundTask> iterator =  ongoingTasks.iterator();
                            while (iterator.hasNext()){
                                BackgroundTask backgroundTask = iterator.next();
                                recoverableTasks.add(backgroundTask);
                                iterator.remove();
                            }
                            ongoingTasks.addFirst(new BackgroundTask(url, downloadListener));
                            ongoingTasks.addAll(recoverableTasks);
                            EFLogger.d(TAG, ">>>||| recover old tasks, total task num:" + ongoingTasks.size());

                            recoverableTasks.clear();

                            workerThread.wakeup();
                            workerThread.setTaskCancelledEventListener(null);
                        }
                    });
                }
            }
        }

    }
    
    private boolean hasExtraWorker() {
        boolean hasRemainingWorker = false;
        for(int i = 0; i< workThreadNum; i++){
            if(workThreads[i].getState().equals(Thread.State.WAITING)){
                hasRemainingWorker = true;
                break;
            }
        }
        return hasRemainingWorker;
    }

    @Override
    public void onStateChange(ConnectivityStateEvent connectivityStateEvent) {
        if(!connectivityStateEvent.isAppOnline()){
            // 
        }else{
            //wakeupWorkers();
        }
    }
}
