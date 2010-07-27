/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.commons.serializer.java;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import org.springframework.commons.serializer.SerializationFailureException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * Delegates to a {@link JavaStreamingConverter} to serialize an object
 * to a byte[]. Source object must implement {@link Serializable}.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class SerializingConverter implements Converter<Object, byte[]> {

	private JavaStreamingConverter converter = new JavaStreamingConverter();
	
	public byte[] convert(Object source) {
		Assert.isTrue(source instanceof Serializable, this.getClass().getName()
				+ " requires a Serializable payload, but received [" + source.getClass().getName() + "]");
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try  {
			this.converter.convert(source, byteStream);
			return byteStream.toByteArray();
		} catch (Exception e) {
			throw new SerializationFailureException("Failed to serialize", e);
		}
	}

}
