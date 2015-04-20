
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

import java.util.concurrent.BlockingQueue;

import org.rayworks.network.download.cache.BaseCache;
import org.rayworks.network.storage.SyncStateStore;
import org.rayworks.network.util.EFLogger;

/***
 * The thread handles downloading remote resource files.
 */
public class WorkerThread extends Thread {
	public static final String TAG = WorkerThread.class.getSimpleName();

	/***
	 * A flag indicates the thread will exist soon
	 */
	private volatile boolean existing = false;

	/***
	 * A flag indicates the thread will rest and stop executing new task
	 */
	private volatile boolean resting = false;
	
	private BlockingQueue<BackgroundTask> mQueue;
	//final private SortedMap<SyncEntity, SyncEntity> mCompleteMap;
	private Downloader downloader;

	private Object restLock = new Object();
    private Object downloaderLock = new Object();

	private final SyncStateStore mStateStore;
	private final BaseCache baseCache;
	private volatile BackgroundTask task;

    /***
     * The event listener to observer the quiting of current task
     */
    public static interface TaskCancelledEventListener {
        void onTaskCancelled();
    }
    private TaskCancelledEventListener taskCancelledEventListener;

    public void setTaskCancelledEventListener(TaskCancelledEventListener taskCancelledEventListener) {
        this.taskCancelledEventListener = taskCancelledEventListener;
    }

    public BackgroundTask getTask() {
		return task;
	}

	public WorkerThread(String name, final BlockingQueue<BackgroundTask> queue, final SyncStateStore syncStateStore, BaseCache cache) {
		super(name);

		mQueue = queue;
		//mCompleteMap = completeTasks;
		mStateStore = syncStateStore;

		baseCache = cache;
	}

	/***
	 * Pauses the tasks and notifies the current thread to quit
	 */
	public void stopNow(){
		cancelRunningTask();

		existing = true;
		interrupt();
	}

	/***
	 * Quits the current executing task
	 */
	public void cancelRunningTask(){
        synchronized (downloaderLock) {
            if (downloader != null) {
                downloader.cancel();
            }
        }
	}

	@Override
	public void run() {
		//TODO: The thread's priority should be set lower to avoid competing with main thread.

		while (!existing) {

			// running state controlled by outside signal
			listenToRestSignal();

			try {
				task = mQueue.take();
				EFLogger.d(TAG, getName() + ">>> Fetch task:" + task);
			} catch (InterruptedException e) {
				// e.printStackTrace();
				if (existing) { // it was time to quit.
					EFLogger.d(TAG, ">>>" + getName() + " is existing now");

					task = null;
					return;
				}
				continue;
			}

			do{
				String remotePath = task.getNextSyncTask();
				boolean targetFileDownloaded = baseCache.existFile(remotePath);

				if(targetFileDownloaded){
					EFLogger.d(TAG, "cache hit for url: " + remotePath);
					task.notifyComplete(remotePath);// filter the path
				}else{
					// Realtime downloading begins
                    if(resting){ // any rescheduling request right now?
                        if(taskCancelledEventListener != null){
                            taskCancelledEventListener.onTaskCancelled();
                        }
                        break;
                    }
                    
                    synchronized (downloaderLock) {
                        downloader = new Downloader(remotePath, mStateStore, baseCache);
                        downloader.setProgressListener(task.getDownloadListener());
                    }
                    DownloadResult downloadResult = downloader.downloadFile();
					if(downloadResult.isOk()) {
						// remove the record of realated temp file
						//runningTask.getSyncStateStore().removeDownloadedFileStamp(entity.getTargetFile().getAbsolutePath()); // synchronized operation

						handleSuccessfulDownload(task, remotePath);
					} else if (downloadResult.isCanceled()) {
						handleTaskCancelled(task, remotePath);
                        if(taskCancelledEventListener != null){
                            taskCancelledEventListener.onTaskCancelled();
                        }
						break;
					} else if (!downloadResult.isRecoverable()) {
						// File does not exist on server, we are unlikely to recover from this
						handleUnrecoverableFailure(task);
						break;

					} else {
						EFLogger.d(TAG, "Error: failed to download, retry it later... url:" + remotePath);
						try {
							// failed to download, retry it late.
							handleCommonFailure(task, downloadResult.getErrorCause());
						}catch (Exception e){
							EFLogger.d(TAG, "Last Retry failed, stop trying for resource #" + remotePath);
							task.notifyError(downloadResult.getErrorCause().getMessage());
							break;
						}

					}

				}
			}while(!existing && task.hasNextSyncTask());

			task = null;
			listenToRestSignal();
		}
	}

	private void listenToRestSignal() {
		try {
            checkSignalToWait();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
	}

	/***
	 * Makes the thread stop executing task and wait
	 */
	public void rest(){
		resting = true;
		cancelRunningTask();
	}

	/***
	 * Wakes up the thread and then continues to process tasks
	 */
	public void wakeup(){
		resting = false;
		synchronized (restLock) {
			restLock.notifyAll();
			EFLogger.d("", ">>> wakeup() invoked #" + getName());
		}
	}

	private void checkSignalToWait() throws InterruptedException{
		synchronized (restLock) {
			while (resting) {
				EFLogger.d("", ">>> before resting #" + getName());
				restLock.wait();
				EFLogger.d("", ">>> after resting, go back to work. #" + getName());
			}
		}
	}

	private void handleTaskCancelled(BackgroundTask task, String remotePath) {
		EFLogger.d("", "task cancelled :" + remotePath);

		// should be configurable
		/*task.reset();
		mQueue.add(task);*/
	}

	/***
	 * Handles common failure
	 * @param task
	 */
	private void handleCommonFailure(BackgroundTask task, Exception e) throws Exception{
		// CompletionType.FAILED, most likely by a network problem

		//sync task failed, lets try to sync it again (Retry strategy?!)
		RetryStrategy retryStrategy = task.getRetryStrategy();
		retryStrategy.retry(e);
	}

	private void handleUnrecoverableFailure(BackgroundTask task) {
		task.notifyError("Error: FAILED_SERVER");
	}

	private void handleSuccessfulDownload(BackgroundTask task, String remotePath) {
		/*SyncEntity syncEntity = runningTask.getEntity();
		syncEntity.setTimestamp(System.currentTimeMillis());*/

		task.notifyComplete(remotePath);
		EFLogger.d("WorkThread", "download complete with url: " + remotePath);
	}
}
