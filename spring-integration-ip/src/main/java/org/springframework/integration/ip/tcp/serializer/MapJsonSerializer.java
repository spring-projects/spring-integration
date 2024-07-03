/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.tcp.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.util.Assert;

/**
 * Serializes a {@link Map} as JSON. Deserializes JSON to
 * a {@link Map}. The default {@link JsonObjectMapperProvider#newInstance()} can be
 * overridden using {@link #setJsonObjectMapper(JsonObjectMapper)}.
 * <p>
 * The JSON deserializer can't delimit multiple JSON
 * objects. Therefore, another (de)serializer is used to
 * apply structure to the stream. By default, this is a
 * simple {@link ByteArrayLfSerializer}, which inserts/expects
 * LF (0x0a) between messages.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 *
 */
public class MapJsonSerializer implements Serializer<Map<?, ?>>, Deserializer<Map<?, ?>> {

	private volatile JsonObjectMapper<?, ?> jsonObjectMapper = JsonObjectMapperProvider.newInstance();

	private volatile Deserializer<byte[]> packetDeserializer = new ByteArrayLfSerializer();

	private volatile Serializer<byte[]> packetSerializer = new ByteArrayLfSerializer();

	/**
	 * An {@link JsonObjectMapper} to be used for the conversion to/from
	 * JSON. Use this if you wish to set additional {@link JsonObjectMapper} implementation features.
	 * @param jsonObjectMapper the jsonObjectMapper.
	 */
	public void setJsonObjectMapper(JsonObjectMapper<?, ?> jsonObjectMapper) {
		Assert.notNull(jsonObjectMapper, "'jsonObjectMapper' cannot be null");
		this.jsonObjectMapper = jsonObjectMapper;
	}

	/**
	 * A {@link Deserializer} that will construct the full JSON content from
	 * the stream which is then passed to the JsonObjectMapper. Default is
	 * {@link ByteArrayLfSerializer}.
	 * @param packetDeserializer the packetDeserializer
	 */
	public void setPacketDeserializer(Deserializer<byte[]> packetDeserializer) {
		Assert.notNull(packetDeserializer, "'packetDeserializer' cannot be null");
		this.packetDeserializer = packetDeserializer;
	}

	/**
	 * A {@link Serializer} that will delimit the full JSON content in
	 * the stream. Default is
	 * {@link ByteArrayLfSerializer}.
	 * @param packetSerializer the packetSerializer
	 */
	public void setPacketSerializer(Serializer<byte[]> packetSerializer) {
		Assert.notNull(packetSerializer, "'packetSerializer' cannot be null");
		this.packetSerializer = packetSerializer;
	}

	@Override
	public Map<?, ?> deserialize(InputStream inputStream) throws IOException {
		byte[] bytes = this.packetDeserializer.deserialize(inputStream);
		try {
			return this.jsonObjectMapper.fromJson(new InputStreamReader(new ByteArrayInputStream(bytes)), Map.class);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void serialize(Map<?, ?> object, OutputStream outputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			this.jsonObjectMapper.toJson(object, new OutputStreamWriter(baos));
		}
		catch (Exception e) {
			throw new IOException(e);
		}
		this.packetSerializer.serialize(baos.toByteArray(), outputStream);
	}

}
