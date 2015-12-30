
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.rayworks.network.download.cache.BaseCache;
import org.rayworks.network.download.exception.HttpRespInvalidState;
import org.rayworks.network.download.exception.ResourceExpiredException;
import org.rayworks.network.download.listener.DownloadListener;
import org.rayworks.network.storage.SyncStateStore;
import org.rayworks.network.util.EFLogger;
import org.rayworks.network.util.IOUtils;
import org.rayworks.network.util.MonitorController;

public class Downloader implements IOUtils.CopyListener {

    private static final String TAG = "Downloader";
    public static final String LAST_MODIFIED = "Last-Modified";
    private byte data[] = new byte[1024];

    private DownloadListener progressListener = null;
    private volatile boolean cancelled;
    private int previewProgress = 0;

    private SyncStateStore syncStateStore;
    private String remoteUrlPath;

    private final BaseCache cache;

    private int progress;

    /**
     * Constructor
     *
     * @param remoteUrlPath
     * @param syncStateStore
     * @param cache
     */
    public Downloader(String remoteUrlPath, SyncStateStore syncStateStore, BaseCache cache) {
        checkNotNull(syncStateStore);
        this.syncStateStore = syncStateStore;
        this.remoteUrlPath = remoteUrlPath;
        this.cache = cache;
    }

    @Override
    public boolean onBytesCopied(int current, int total) {
        if (progressListener != null) {
            int percentageComplete = (int) (current * 100.0f / total);
            if (progress != percentageComplete) {
                progress = percentageComplete;
                progressListener.onProgress(progress, remoteUrlPath);
            }
        }
        return !cancelled;
    }

    /**
     * Do download remote resource
     *
     * @return
     */
    public DownloadResult downloadFile() {
        EFLogger.v(TAG, "downloadFile, url=" + remoteUrlPath);
        synchronized (MonitorController.getInstance().get(remoteUrlPath)) {

            // download the file
            try {
                URL url = new URL(remoteUrlPath);
                if(cache.existFile(remoteUrlPath)){
                    EFLogger.d(TAG, "Cache hit for " + remoteUrlPath + ", abort downloading again.");

                    File file = cache.getFile(remoteUrlPath);
                    return DownloadResult.createSuccessfulDownloadResult(url, file, file.length());
                }

                File targetFile = cache.getTempFile(remoteUrlPath);
                HttpURLConnection connection = connect(remoteUrlPath, targetFile);

                long start = 0;
                int responseCode = connection.getResponseCode();

                if (responseCode < 200 || responseCode > 299) {
                    throw new HttpRespInvalidState("Bad http response status code " + responseCode, responseCode);
                }

                EFLogger.d(TAG, ">>> resp code " + responseCode);

                if (responseCode == 206) {
                    // The downloaded content can be appended to the existing file.
                    start = targetFile.length();
                }

                String location = connection.getHeaderField("Location");
                if (location != null) {
                    // We have been redirected. This is typically a guard for the "Starbucks" case.
                    if (!url.equals(getUrlForLocation(location))) {
                        EFLogger.v("Downloader", "Redirected, download will fail");
                        return DownloadResult.createFailedDownloadResult(null);
                    }
                }

                // Note: content-length is the size to be downloaded, not total file size
                // which will be different in the case of resuming a download
                long size = start + connection.getContentLength();

                // If successful, download returns the total file size
                long total = download(connection.getInputStream(), start, size);

                if (total == size) {
                    // All the data was copied
                    return DownloadResult.createSuccessfulDownloadResult(url, targetFile, size);
                } else {
                    // Not all the data was copied, but no error, so download was cancelled
                    return DownloadResult.createCancelDownloadResult();
                }
            } catch (MalformedURLException e) { // bad url
                return DownloadResult.createUnrecoverableErrorResult(e);
            } catch (IOException e) {
                e.printStackTrace();
                return DownloadResult.createFailedDownloadResult(e);
            } catch (HttpRespInvalidState e) {
                EFLogger.d(TAG, "IOException:" + e.getMessage());
                return DownloadResult.createUnrecoverableErrorResult(e);
            } catch (ResourceExpiredException e) {
                EFLogger.d(TAG, "Resource Expired:" + remoteUrlPath);
                return DownloadResult.createFailedDownloadResult(e);
            }
        }
    }

    private URL getUrlForLocation(String location) {
        try {
            return new URL(location);
        } catch (MalformedURLException e) {
            EFLogger.d(TAG, "Bad url:" + location);
            return null;
        }
    }

    /**
     * Generates a httpUrlConnection instance.
     * <ul>
     * <li>1. In order to support downloading from breakpoint, the range will be calculated and set to the http header
     * according to the partially downloaded file if has any.
     * </li>
     * <li>
     * 2. The lastModified value needs to be verified before merging the partial data into local target file.
     * If the values are not consistent, the local temp file should be dumped and a new task for downloading will be
     * triggered.
     * </li>
     * </ul>
     *
     * @param remoteUrlPath
     * @param targetFile
     * @return
     * @throws IOException
     * @throws ResourceExpiredException
     */
    private HttpURLConnection connect(final String remoteUrlPath, File targetFile) throws IOException, ResourceExpiredException {
        // If the target file exists, check that this was previously downloaded
        HttpURLConnection urlConnection;
        urlConnection = (HttpURLConnection) new URL(remoteUrlPath).openConnection();

        // TODO: make value of timeout configurable
        urlConnection.setConnectTimeout(30000);
        urlConnection.setReadTimeout(30000);
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.setUseCaches(false);
        urlConnection.setDoInput(true);

        String lastModifiedString = null;
        if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
            urlConnection.setRequestProperty("Range", "bytes=" + targetFile.length() + "-");

            urlConnection.connect();
            lastModifiedString = urlConnection.getHeaderField(LAST_MODIFIED);

            if (lastModifiedString != null && !lastModifiedString.equals(syncStateStore.getDownloadedFileStamp(remoteUrlPath))) {
                throw new ResourceExpiredException();
            }
        } else {
            // Initial download.
            urlConnection.connect();
            lastModifiedString = urlConnection.getHeaderField(LAST_MODIFIED);
            if (lastModifiedString != null) {
                syncStateStore.setDownloadedFileStamp(remoteUrlPath, lastModifiedString);
            }
        }

        return urlConnection;
    }

    private long download(InputStream inputStream, long start, long size) throws IOException {
        InputStream input = new BufferedInputStream(inputStream);

        EFLogger.d(TAG, "start=" + start + ",size=" + size);

        boolean loaded = false;
        long total = start;
        try {
            loaded = cache.save(remoteUrlPath, input, (int) size, this);
            if (loaded) {
                if (progressListener != null) {
                    syncStateStore.removeDownloadedFileStamp(remoteUrlPath);// remove record when downloading is complete

                    progressListener.onProgress(100, remoteUrlPath);
                    progressListener.onComplete(remoteUrlPath);
                }
            }
        } finally {
            if (loaded) {
                total = cache.getFile(remoteUrlPath).length();
            } else {
                total = cache.getTempFile(remoteUrlPath).length();
            }
            IOUtils.closeSilently(input);
        }

        return total;
    }

    public void setProgressListener(DownloadListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Cancels the downloading execution
     * <p>NB: Once the method gets called, the current Downloader instance will not be available any more.</p>
     */
    public void cancel() {
        cancelled = true;
    }
}
