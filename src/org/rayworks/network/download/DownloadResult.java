
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

import java.io.File;
import java.net.URL;

public class DownloadResult {

	private final URL url;
	private final File resultingFile;
	private final long total;
	private final boolean ok;
	private final boolean canceled;
	private final Exception errorCause;
	private final boolean recoverable;

	private DownloadResult(URL url, File resultingFile, long total, boolean ok, boolean canceled, Exception errorCause) {
		/*this.url = url;
		this.resultingFile = resultingFile;
		this.total = total;
		this.ok = ok;
		this.canceled = canceled;
		this.errorCause = errorCause;
		this.recoverable = true;*/
		this(url, resultingFile, total, ok, canceled, true, errorCause);
	}

	private DownloadResult(URL url, File resultingFile, long total, boolean ok, boolean canceled, boolean recoverable, Exception errorCause){
		this.url = url;
		this.resultingFile = resultingFile;
		this.total = total;
		this.ok = ok;
		this.canceled = canceled;
		this.errorCause = errorCause;
		this.recoverable = recoverable;
	}

	public static DownloadResult createSuccessfulDownloadResult(URL url, File resultingFile, long total) {
		
		return new DownloadResult(url, resultingFile, total, true, false, null);
	}
	
	public static DownloadResult createFailedDownloadResult(Exception e) {
		
		return new DownloadResult(null, null, 0, false, false, e);
	}
	
	public static DownloadResult createCancelDownloadResult() {
		return new DownloadResult(null, null, 0, false, true, null);
	}

	public static DownloadResult createUnrecoverableErrorResult(Exception e){
		return new DownloadResult(null, null, 0, false, false, false, e);
	}

	public URL getUrl() {
		return url;
	}

	public File getResultingFile() {
		return resultingFile;
	}

	public long getTotal() {
		return total;
	}

	public boolean isOk() {
		return ok;
	}
	
	public boolean isCanceled() {
		return canceled;
	}

	public boolean isRecoverable() {
		return recoverable;
	}

	public Exception getErrorCause() {
		return errorCause;
	}

}
