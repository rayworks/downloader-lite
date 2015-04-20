package org.rayworks.network.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import org.rayworks.network.storage.KeyedObject;
import org.rayworks.network.storage.KeyedObjectStoreLite;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class FileKvStoreLite implements KeyedObjectStoreLite {

	JsonWriter writer;
	JsonReader reader;

	public FileKvStoreLite() {
	}

	@Override
	public void save(String key, String value) {

	}

	@Override
	public String get(String key) {
		return null;
	}

	@Override
	public void remove(String key) {

	}

	@Override
	public Map<String, String> getAll() {
		return null;
	}

	@Override
	public void clear() {

	}

	@Override
	public void store(KeyedObject object) {

	}

	@Override
	public <E> E get(String key, Class<E> cls) {
		return null;
	}

	@Override
	public Map<String, Object> getAllObjects() {
		return null;
	}

}
