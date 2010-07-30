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

import java.io.ByteArrayOutputStream;

import org.springframework.core.convert.converter.Converter;

/**
 * A {@Link Converter} that delegates to a {@link OutputStreamingConverter}
 * to convert an object to a byte[]. 
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class SerializingConverter implements Converter<Object, byte[]> {

	private OutputStreamingConverter<Object> streamingConverter; 
	
	/**
	 * @param streamingConverter the OutputStreamingConverter
	 */
	public SerializingConverter(OutputStreamingConverter<Object> streamingConverter) {
		this.streamingConverter = streamingConverter;
	}
	
	public byte[] convert(Object source) {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(128);
		try  {
			this.streamingConverter.convert(source, byteStream);
			return byteStream.toByteArray();
		} catch (Exception e) {
			throw new SerializationFailureException("Failed to serialize", e);
		}
	}

}
