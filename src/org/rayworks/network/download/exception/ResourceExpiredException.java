
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

package org.rayworks.network.download.exception;

/**
 * Created by seanzhou on 12/23/14.
 *
 * An exception indicates the resource with the same uri has been modified.
 */

public class ResourceExpiredException extends DownloadException {
    public ResourceExpiredException() {

    }

    public ResourceExpiredException(String message) {
        super(message);
    }
}
