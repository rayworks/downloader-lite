package org.rayworks.network.test;

import java.util.HashMap;
import java.util.Map;

import org.rayworks.network.storage.KeyedObject;
import org.rayworks.network.storage.KeyedObjectStoreLite;

public class KeyedObjectStoreLiteImpl implements KeyedObjectStoreLite {

	Map<String, Object> maps = new HashMap<>();
	@Override
	public void save(String key, String value) {
		maps.put(key, value);

	}

	@Override
	public String get(String key) {
		return maps.get(key).toString();
	}

	@Override
	public void remove(String key) {
		maps.remove(key);

	}

	@Override
	public Map<String, String> getAll() {
		return null;
	}

	@Override
	public void clear() {
		maps.clear();

	}

	@Override
	public void store(KeyedObject object) {
		maps.put(object.getKey(), object);
	}

	@Override
	public <E> E get(String key, Class<E> cls) {
		Object obj = maps.get(key);
		return (E)obj;
	}

	@Override
	public Map<String, Object> getAllObjects() {
		return maps;
	}

}
