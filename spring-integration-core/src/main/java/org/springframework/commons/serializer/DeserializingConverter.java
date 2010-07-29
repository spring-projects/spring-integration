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

package org.springframework.commons.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.commons.serializer.java.JavaStreamingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * Delegates to a {@link InputStreamingConverter} (default is 
 * {@link JavaStreamingConverter}} to deserialize data
 * in a byte[] to an object.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class DeserializingConverter implements Converter<byte[], Object> {

	private InputStreamingConverter<Object> streamingConverter 
				= new JavaStreamingConverter();
	
	public Object convert(byte[] source) {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(source);
		try {
			return streamingConverter.convert(byteStream);
		}
		catch (Exception e) {
			try {
				byteStream.close();
			} catch (IOException e1) { }
			throw new DeserializationFailureException(
					"Failed to deserialize payload. Is the byte array a result of Object serialization?", e);
		}
	}

	/**
	 * Override the default {@link JavaStreamingConverter}
	 * @param streamingConverter the streamingConverter to set
	 */
	public void setStreamingConverter(InputStreamingConverter<Object> streamingConverter) {
		this.streamingConverter = streamingConverter;
	}


}
