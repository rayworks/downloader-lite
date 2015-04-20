package org.rayworks.network.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.rayworks.network.storage.KeyValueStore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class JsonKVStore implements KeyValueStore {
	private final String TAG = JsonKVStore.class.getSimpleName();
	
	private File cacheDir;
	private File record;
	private Map<String, String> map = new HashMap<String, String>();
	private Type mapType = new TypeToken<Map<String, String>>() {}.getType();
	private Gson gson;
	
	public JsonKVStore(String cacheFileRoot) {
		gson = new Gson();
		cacheDir = new File(cacheFileRoot);
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		record = new File(cacheDir, "record.json");
		if (record.exists()) {
			JsonReader reader;
			try {
				reader = new JsonReader(new FileReader(record));
				map = gson.fromJson(reader, mapType);

				System.out.println(TAG +" read content from file");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		}
	}

	@Override
	public void save(String key, String value) {
		map.put(key, value);
		writeToFile();
	}

	private void writeToFile() {
		try {
			if (!record.exists()) {
				record.createNewFile();
			}
			OutputStream out = new FileOutputStream(record);
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
			gson.toJson(map, mapType, writer);
			writer.flush();
			writer.close();
			
			System.out.println(TAG+ " Modification saved to file");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String get(String key) {
		return map.get(key);
	}

	@Override
	public void remove(String key) {
		map.remove(key);
		writeToFile();
	}

	@Override
	public Map<String, String> getAll() {
		return map;
	}

	@Override
	public void clear() {
		map.clear();
		writeToFile();
	}

}
