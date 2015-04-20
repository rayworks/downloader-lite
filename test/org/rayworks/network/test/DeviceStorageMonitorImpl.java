package org.rayworks.network.test;

import org.rayworks.service.DeviceStorageMonitor;

public class DeviceStorageMonitorImpl implements DeviceStorageMonitor {

	@Override
	public void addStorageMonitorListener(StorageMonitorListener listener) {

	}

	@Override
	public void removeStorageMonitorListener(StorageMonitorListener listener) {

	}

	@Override
	public boolean isStorageLimitedReached() {
		return false;
	}

}
