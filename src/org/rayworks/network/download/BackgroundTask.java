
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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import org.rayworks.network.download.listener.DownloadListener;

public class BackgroundTask {

    private String syncTask;
    private WeakReference<DownloadListener> downloadListenerRef;

    public DownloadListener getDownloadListener() {
        return downloadListenerRef == null ? null : downloadListenerRef.get();
    }

    private boolean compoundTask;

    public boolean isCompoundTask() {
        return compoundTask;
    }

    private List<String> syncTasks;

    private RetryStrategy retryStrategy;

    /***
     *
     * @return RetryStrategy Not null
     */
    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
    }

    private String tag = "";

    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public BackgroundTask copyInstance(){
        BackgroundTask bkgTask = this.syncTask == null ? new BackgroundTask(this.syncTasks, getDownloadListener()) :
                new BackgroundTask(this.syncTask, getDownloadListener());
        
        bkgTask.index = 0;
        bkgTask.setTag(this.tag);
        
        return bkgTask;
    }

    public BackgroundTask(List<String> syncTasks, DownloadListener downloadListener) {
        init(syncTasks, null, downloadListener);
    }

    private void init(List<String> syncTasks, String syncTask, DownloadListener downloadListener) {
        // check the list size
        compoundTask = syncTasks != null;
        this.syncTasks = syncTasks;
        this.syncTask = syncTask;

        if (downloadListener != null) {
            downloadListenerRef = new WeakReference<>(downloadListener);
        }

        retryStrategy = new DefaultRetryStrategy(this);
        index = 0;
    }

    public BackgroundTask(String syncTask, DownloadListener downloadListener) {
        init(null, syncTask, downloadListener);
    }

    private int index = 0;
    
    public void releaseListener(){
        if(downloadListenerRef != null) {
            downloadListenerRef.clear();
            downloadListenerRef = null;
        }
    }

    public boolean containTargetKey(String url) {
        if (!compoundTask) {
            return syncTask.equals(url);
        } else {
            for (String task : syncTasks) {
                if (task.equals(url)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Prepares the state of current task for re-executing
     */
    /*public*/ void reset() {
        index = 0;
    }

    public String getNextSyncTask() {
        if (compoundTask && index < syncTasks.size()) {
            String task = syncTasks.get(index);
            ++index;
            System.out.println("Task index changed to : " + index);
            return task;
        }

        if(!compoundTask){
            ++index;
        }
        return syncTask;
    }

    public boolean hasNextSyncTask() {
        if (compoundTask) {
            if (syncTasks != null && index == syncTasks.size()) {
                return false;
            }
            return true;
        }else{
            return index == 0;
        }
    }
	
	/*public void notifyProgress(int progress){
		if(downloadListenerRef != null && downloadListenerRef.get()!= null){
			downloadListenerRef.get().onProgress(ProgressType.INCREMENT, progress, "");
		}
	}*/

    public void notifyComplete(String remotePath) {
        if (downloadListenerRef != null && downloadListenerRef.get() != null) {
            downloadListenerRef.get().onComplete(remotePath);
        }
    }

    public void notifyError(String error) {
        if (downloadListenerRef != null && downloadListenerRef.get() != null) {
            downloadListenerRef.get().onError(error);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Compound:").append(" | ").append("SyncTask:")
                .append(compoundTask ? Arrays.toString(syncTasks
                        .toArray(new String[syncTasks.size()])) : syncTask);
        return builder.toString();
    }

}
