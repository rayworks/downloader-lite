
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

import org.rayworks.network.util.EFLogger;

/**
 * Created by seanzhou on 12/30/14.
 */
public class DefaultRetryStrategy implements RetryStrategy {
    private final String TAG = DefaultRetryStrategy.class.getName();

    private final int MAX_RETRY_TIMES = 3;
    private int timesTried;
    private final BackgroundTask backgroundTask;

    public DefaultRetryStrategy(final BackgroundTask backgroundTask) {
        this.backgroundTask = backgroundTask;
    }

    @Override
    public boolean reachedMaxRetryTimes() {
        return timesTried > MAX_RETRY_TIMES;
    }

    @Override
    public void retry(Exception exception) throws Exception {
        ++timesTried;
        if (!reachedMaxRetryTimes()) {
            EFLogger.d(TAG, "retry for time " + timesTried);

            backgroundTask.reset();
            // Another way is to change the 'Timeout value' for the next round task execution
        } else {
            throw exception;
        }
    }
}
