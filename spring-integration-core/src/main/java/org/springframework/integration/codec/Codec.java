/*
 * Copyright 2013 the original author or authors.
 *
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

package org.springframework.integration.codec;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.serializer.Serializer;


/**
 * Interface for classes that perform both serialization and deserialization on multiple classes.
 * @author David Turanski
 * @since 4.1
 */
public interface Codec extends Serializer<Object> {

	/**
	 * Deserialize an object of a given type
	 * @param inputStream the input stream containing the serialized object
	 * @param type the object's class
	 * @return the object
	 * @throws IOException if the operation fails
	 */
	public abstract Object deserialize(InputStream inputStream, Class<?> type) throws IOException;

	/**
	 * Deserialize an object of a given type
	 * @param bytes the byte array containing the serialized object
	 * @param type the object's class
	 * @return the object
	 * @throws IOException if the operation fails
	 */
	public abstract Object deserialize(byte[] bytes, Class<?> type) throws IOException;

	/**
	 * Serialize an object to a byte array
	 * @param object the object to serialize
	 * @return the bytes
	 * @throws IOException if the operation fails
	 */
	public abstract byte[] serialize(Object object) throws IOException;
}
