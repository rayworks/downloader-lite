
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

package org.rayworks.network.storage;


/***
 * Persist the lastModified timestamp of the current downloading files 
 * @author Sean
 *
 */
public class SyncStateStore {
	
    private static final String SYNC_FILESTAMP_PREFIX = "sss_#filestamp#";

    private KeyValueStore store;

	public SyncStateStore(KeyValueStore store) {
        this.store = store;
	}
	
    public void setDownloadedFileStamp(String targetDir, String lastModifiedString) {
        store.save(SYNC_FILESTAMP_PREFIX + targetDir, lastModifiedString);
    }

    public String getDownloadedFileStamp(String targetDir) {
        return store.get(SYNC_FILESTAMP_PREFIX + targetDir);
    }

    public void removeDownloadedFileStamp(String targetDir){
        store.remove(SYNC_FILESTAMP_PREFIX + targetDir);
    }
}
