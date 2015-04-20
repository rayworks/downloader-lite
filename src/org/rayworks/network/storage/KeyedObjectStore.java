
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

import java.lang.reflect.Type;
import java.util.Map;

public interface KeyedObjectStore {

	void store(KeyedObject object);

	void store(String key, Object object);

	<E> E get(String key, Class<E> cls);

	<E> E get(String key, Type type);

	void clear();

	void beginBatchCommit();

	void endBatchCommit();

	void storeString(String key, String string);

	String getString(String key);

	long getLong(String lastSettingsCkeck);

	void storeLong(String key, long longValue);

	Map<String, Object> getAll();

	boolean getBoolean(String key, boolean defaultValue);

	void storeBoolean(String key, boolean value);

	int getInt(String key, int defaultValue);

	void storeInt(String key, int value);

	void remove(String key);
}
