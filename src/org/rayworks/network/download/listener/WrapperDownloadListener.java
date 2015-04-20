
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

package org.rayworks.network.download.listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A decorated {@link DownloadListener} who indicates the progress for a
 * composite task.
 * {Hide}
 *
 * @author seanzhou
 */
public final class WrapperDownloadListener implements DownloadListener {

    private final boolean compound;
    //private ArrayList<File> files = new ArrayList<>();

    private final ArrayList<String> files = new ArrayList<>();
    private DownloadListener mListener;


    public WrapperDownloadListener(DownloadListener listener, List<String> remoteUrls) {
        compound = remoteUrls.size() > 1;
        mListener = listener;

        files.addAll(remoteUrls);
    }

    /**
     * Gets the actual progress for batched task downloading.
     *
     * @param currProgress
     * @param remotePath
     * @return
     */
    private int checkCompoundProgress(int currProgress, String remotePath) {
        int cnt = files.size();
        int pos = 0;
        for (int i = 0; i < cnt; i++) {
            if (files.get(i).equals(remotePath)) {
                pos = i;
                break;
            }
        }
        int totalFileCnt = files.size();
        int rate = (int) (currProgress * 1.0 / totalFileCnt + 100.0 / totalFileCnt * pos);
        return rate;
    }


    @Override
    public void onProgress(int percentageComplete, String remotePath) {
        if (compound) {
            mListener.onProgress(
                    checkCompoundProgress(percentageComplete, remotePath), remotePath);
        } else {
            mListener.onProgress(percentageComplete, remotePath);
        }
    }

    private int locateIndexOfDownloadedFile(String remoteFilePath) {
        for (int i = 0; i < files.size(); i++) {
            if ((files.get(i).equals(remoteFilePath))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onComplete(String remotePath) {
        if (compound) {
            boolean allComplete;
            int i = locateIndexOfDownloadedFile(remotePath);
            allComplete = i == files.size() - 1;

            if (allComplete) {
                System.out.println("Batched downloading complete!!!");
                //mListener.onProgress(100, remotePath, localPath);
                mListener.onComplete(remotePath);
            } else {
                mListener.onProgress((int) ((i + 1) * 1.0 / files.size() * 100), remotePath);
            }
        } else {
            mListener.onComplete(remotePath);
        }
    }

    @Override
    public void onError(String error) {
        System.err.println("Error: " + error);
        mListener.onError(error);
    }


}
