package org.rayworks.network.test;

import java.util.HashMap;
import java.util.Map;

import org.rayworks.network.storage.KeyValueStore;

public class KeyValueStoreImpl implements KeyValueStore {

	Map<String, String> maps = new HashMap<>();
	@Override
	public void save(String key, String value) {
		maps.put(key, value);
	}

	@Override
	public String get(String key) {
		return maps.get(key);
	}

	@Override
	public void remove(String key) {
		maps.remove(key);
	}

	@Override
	public Map<String, String> getAll() {
		return maps;
	}

	@Override
	public void clear() {
		maps.clear();
	}

}
