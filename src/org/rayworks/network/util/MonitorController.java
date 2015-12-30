package org.rayworks.network.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Created by seanzhou on 12/30/15.
 * A monitor based on {@link java.util.HashMap}. It caches the monitor object for a specified key.
 */
public class MonitorController {
    private static MonitorController sMonitor;

    private MonitorController() {

    }

    public synchronized static MonitorController getInstance() {
        if (sMonitor == null) {
            sMonitor = new MonitorController();
        }
        return sMonitor;
    }

    private WeakHashMap<Object, Object> map = new WeakHashMap<Object, Object>() {
        @Override
        public Object get(Object key) {
            Object ref = super.get(key);
            Object monitor = (ref == null) ? null : ((WeakReference) ref).get();
            if (monitor == null) {
                monitor = key;
                put(monitor, new WeakReference<>(monitor));
            }
            return monitor;
        }
    };

    public synchronized Object get(Object key) {
        return map.get(key);
    }
}
