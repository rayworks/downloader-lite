
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

public class DownloadSetting {

    private int threadPriority;
    private int timeout;
    private int threadNum;
    private long maxSizeForDownloadContent;
    private long minSizeKeptForDeviceStorage;


    private DownloadEnabledStrategy downloadEnabledStrategy;

    public int getThreadNum() {
        return threadNum;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public long getMaxSizeForDownloadContent() {
        return maxSizeForDownloadContent;
    }

    public long getMinSizeKeptForDeviceStorage() {
        return minSizeKeptForDeviceStorage;
    }

    public DownloadEnabledStrategy getDownloadEnabledStrategy() {
        return downloadEnabledStrategy;
    }

    private DownloadSetting(Builder builder) {

        this.timeout = builder.timeout;
        this.threadNum = builder.threadNum;
        this.threadPriority = builder.threadPriority;
        this.maxSizeForDownloadContent = builder.maxSizeForDownloadContent;
        this.minSizeKeptForDeviceStorage = builder.minSizeKeptForDeviceStorage;
        this.downloadEnabledStrategy = builder.downloadEnabledStrategy;
    }

    public static class Builder {
        private int timeout = 10 * 1000; // in Milliseconds
        private int threadNum = 3;
        private int threadPriority = Integer.MIN_VALUE;
        private long maxSizeForDownloadContent = 1024 * 1024 * 450L;
        private long minSizeKeptForDeviceStorage = 100 * 1024 * 1024L;
        private DownloadEnabledStrategy downloadEnabledStrategy;

        public Builder() {

        }

        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setThreadNum(int threadNum) {
            this.threadNum = threadNum;
            return this;
        }

        public Builder setThreadPriority(int threadPriority) {
            this.threadPriority = threadPriority;
            return this;
        }

        public Builder setDownloadEnabledStrategy(DownloadEnabledStrategy downloadEnabledStrategy) {
            this.downloadEnabledStrategy = downloadEnabledStrategy;
            return this;
        }

        public Builder setMaxSizeForDownloadContent(long maxSizeForDownloadContent) {
            this.maxSizeForDownloadContent = maxSizeForDownloadContent;
            return this;
        }

        public Builder setMinSizeKeptForDeviceStorage(long minSizeKeptForDeviceStorage) {
            this.minSizeKeptForDeviceStorage = minSizeKeptForDeviceStorage;
            return this;
        }

        public DownloadSetting create() {
            if (threadPriority == Integer.MIN_VALUE) {
                throw new IllegalArgumentException("The thread priority should be set properly");
            }

            if (downloadEnabledStrategy == null) {
                throw new IllegalArgumentException("The download strategy should be specified");
            }

            return new DownloadSetting(this);
        }
    }
}
