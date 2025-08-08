/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for classes that perform both encode (serialize) and decode (deserialize) on multiple classes.
 *
 * @author David Turanski
 * @since 4.2
 */
public interface Codec {

	/**
	 * Encode (encode) an object to an OutputStream.
	 * @param object the object to encode
	 * @param outputStream the OutputStream
	 * @throws IOException if the operation fails
	 */
	void encode(Object object, OutputStream outputStream) throws IOException;

	/**
	 * Encode an object to a byte array.
	 * @param object the object to encode
	 * @return the bytes
	 * @throws IOException if the operation fails
	 */
	byte[] encode(Object object) throws IOException;

	/**
	 * Decode an object of a given type.
	 * @param inputStream the input stream containing the encoded object
	 * @param type the object's class
	 * @param <T> the object's type
	 * @return the object
	 * @throws IOException if the operation fails
	 */
	<T> T decode(InputStream inputStream, Class<T> type) throws IOException;

	/**
	 * Decode an object of a given type.
	 * @param bytes the byte array containing the encoded object
	 * @param type the object's class
	 * @param <T> the object's type
	 * @return the object
	 * @throws IOException if the operation fails
	 */
	<T> T decode(byte[] bytes, Class<T> type) throws IOException;

}
