
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

import org.rayworks.network.download.listener.DownloadListener;

public class DownloadRequest {
	private boolean compoundRequest = false;
	public DownloadRequest(String url, String localStoragePath, DownloadListener downloadListener) {
		this.url = url;
		this.localStoragePath = url;
		this.downloadListener = downloadListener;
	}
	
	private String url;
	private String localStoragePath;
	private DownloadListener downloadListener;
	
	private int tagId = -1;
	
	public int getTagId() {
		return tagId;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getLocalStoragePath() {
		return localStoragePath;
	}
	
	public DownloadListener getDownloadListener() {
		return downloadListener;
	}
}
