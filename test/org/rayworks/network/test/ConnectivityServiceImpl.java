package org.rayworks.network.test;

import org.rayworks.service.ConnectivityService;
import org.rayworks.service.ConnectivityState;

public class ConnectivityServiceImpl implements ConnectivityService {
    @Override
    public boolean isAppOnline() {
        return true;
    }

    @Override
    public ConnectivityState getConnectivityState() {
        return ConnectivityState.ONLINE;
    }

    @Override
    public void addListener(ConnectivityStateEventListener listener) {

    }

    @Override
    public void removeListener(ConnectivityStateEventListener listener) {

    }

}
