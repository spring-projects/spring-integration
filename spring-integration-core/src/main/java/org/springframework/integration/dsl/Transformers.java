/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.function.Function;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.transformer.DecodingTransformer;
import org.springframework.integration.transformer.EncodingPayloadTransformer;
import org.springframework.integration.transformer.MapToObjectTransformer;
import org.springframework.integration.transformer.ObjectToMapTransformer;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.integration.transformer.PayloadDeserializingTransformer;
import org.springframework.integration.transformer.PayloadSerializingTransformer;
import org.springframework.integration.transformer.PayloadTypeConvertingTransformer;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.integration.transformer.SyslogToMapTransformer;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An utility class to provide methods for out-of-the-box
 * {@link org.springframework.integration.transformer.Transformer}s.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class Transformers {

	private final static SpelExpressionParser PARSER = new SpelExpressionParser();

	public static ObjectToStringTransformer objectToString() {
		return objectToString(null);
	}

	public static ObjectToStringTransformer objectToString(String charset) {
		return charset != null ? new ObjectToStringTransformer(charset) : new ObjectToStringTransformer();
	}

	public static ObjectToMapTransformer toMap() {
		return new ObjectToMapTransformer();
	}

	public static ObjectToMapTransformer toMap(boolean shouldFlattenKeys) {
		ObjectToMapTransformer transformer = new ObjectToMapTransformer();
		transformer.setShouldFlattenKeys(shouldFlattenKeys);
		return transformer;
	}

	public static ObjectToMapTransformer toMap(JsonObjectMapper<?, ?> jsonObjectMapper) {
		return new ObjectToMapTransformer(jsonObjectMapper);
	}

	public static ObjectToMapTransformer toMap(JsonObjectMapper<?, ?> jsonObjectMapper, boolean shouldFlattenKeys) {
		ObjectToMapTransformer transformer = new ObjectToMapTransformer();
		transformer.setShouldFlattenKeys(shouldFlattenKeys);
		return transformer;
	}

	public static MapToObjectTransformer fromMap(Class<?> targetClass) {
		return new MapToObjectTransformer(targetClass);
	}

	public static MapToObjectTransformer fromMap(String beanName) {
		return new MapToObjectTransformer(beanName);
	}

	public static ObjectToJsonTransformer toJson() {
		return toJson(null, null, null);
	}

	public static ObjectToJsonTransformer toJson(JsonObjectMapper<?, ?> jsonObjectMapper) {
		return toJson(jsonObjectMapper, null, null);
	}

	public static ObjectToJsonTransformer toJson(JsonObjectMapper<?, ?> jsonObjectMapper,
			ObjectToJsonTransformer.ResultType resultType) {
		return toJson(jsonObjectMapper, resultType, null);
	}

	public static ObjectToJsonTransformer toJson(String contentType) {
		return toJson(null, null, contentType);
	}

	public static ObjectToJsonTransformer toJson(JsonObjectMapper<?, ?> jsonObjectMapper, String contentType) {
		return toJson(jsonObjectMapper, null, contentType);
	}

	public static ObjectToJsonTransformer toJson(ObjectToJsonTransformer.ResultType resultType, String contentType) {
		return toJson(null, resultType, contentType);
	}

	public static ObjectToJsonTransformer toJson(JsonObjectMapper<?, ?> jsonObjectMapper,
			ObjectToJsonTransformer.ResultType resultType, String contentType) {
		ObjectToJsonTransformer transformer;
		if (jsonObjectMapper != null) {
			if (resultType != null) {
				transformer = new ObjectToJsonTransformer(jsonObjectMapper, resultType);
			}
			else {
				transformer = new ObjectToJsonTransformer(jsonObjectMapper);
			}
		}
		else if (resultType != null) {
			transformer = new ObjectToJsonTransformer(resultType);
		}
		else {
			transformer = new ObjectToJsonTransformer();
		}
		if (contentType != null) {
			transformer.setContentType(contentType);
		}
		return transformer;
	}

	public static JsonToObjectTransformer fromJson() {
		return fromJson(null, null);
	}

	public static JsonToObjectTransformer fromJson(Class<?> targetClass) {
		return fromJson(targetClass, null);
	}

	public static JsonToObjectTransformer fromJson(JsonObjectMapper<?, ?> jsonObjectMapper) {
		return fromJson(null, jsonObjectMapper);
	}

	public static JsonToObjectTransformer fromJson(Class<?> targetClass, JsonObjectMapper<?, ?> jsonObjectMapper) {
		return new JsonToObjectTransformer(targetClass, jsonObjectMapper);
	}

	public static PayloadSerializingTransformer serializer() {
		return serializer(null);
	}

	public static PayloadSerializingTransformer serializer(Serializer<Object> serializer) {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		if (serializer != null) {
			transformer.setSerializer(serializer);
		}
		return transformer;
	}

	public static PayloadDeserializingTransformer deserializer() {
		return deserializer(null);
	}

	public static PayloadDeserializingTransformer deserializer(Deserializer<Object> deserializer) {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		if (deserializer != null) {
			transformer.setDeserializer(deserializer);
		}
		return transformer;
	}

	public static <T, U> PayloadTypeConvertingTransformer<T, U> converter(Converter<T, U> converter) {
		Assert.notNull(converter, "The Converter<?, ?> is required for the PayloadTypeConvertingTransformer");
		PayloadTypeConvertingTransformer<T, U> transformer = new PayloadTypeConvertingTransformer<>();
		transformer.setConverter(converter);
		return transformer;
	}

	public static SyslogToMapTransformer syslogToMap() {
		return new SyslogToMapTransformer();
	}

	/**
	 * The factory method for the {@link EncodingPayloadTransformer}.
	 * @param codec the {@link Codec} to use.
	 * @param <T> the {@code payload} type.
	 * @return the {@link EncodingPayloadTransformer} instance.
	 */
	public static <T> EncodingPayloadTransformer<T> encoding(Codec codec) {
		return new EncodingPayloadTransformer<>(codec);
	}

	/**
	 * The factory method for the {@link DecodingTransformer}.
	 * @param codec the {@link Codec} to use.
	 * @param type the target type to transform to.
	 * @param <T> the target type.
	 * @return the {@link DecodingTransformer} instance.
	 */
	public static <T> DecodingTransformer<T> decoding(Codec codec, Class<T> type) {
		return new DecodingTransformer<>(codec, type);
	}

	/**
	 * The factory method for the {@link DecodingTransformer}.
	 * @param codec the {@link Codec} to use.
	 * @param typeExpression the target type SpEL expression.
	 * @param <T> the target type.
	 * @return the {@link DecodingTransformer} instance.
	 */
	public static <T> DecodingTransformer<T> decoding(Codec codec, String typeExpression) {
		return decoding(codec, PARSER.parseExpression(typeExpression));
	}

	/**
	 * The factory method for the {@link DecodingTransformer}.
	 * @param codec the {@link Codec} to use.
	 * @param typeFunction the target type function.
	 * @param <T> the target type.
	 * @return the {@link DecodingTransformer} instance.
	 */
	public static <T> DecodingTransformer<T> decoding(Codec codec, Function<Message<?>, Class<T>> typeFunction) {
		return decoding(codec, new FunctionExpression<>(typeFunction));
	}

	/**
	 * The factory method for the {@link DecodingTransformer}.
	 * @param codec the {@link Codec} to use.
	 * @param typeExpression the target type SpEL expression.
	 * @param <T> the target type.
	 * @return the {@link DecodingTransformer} instance.
	 */
	public static <T> DecodingTransformer<T> decoding(Codec codec, Expression typeExpression) {
		return new DecodingTransformer<>(codec, typeExpression);
	}

	/**
	 * The factory method for the {@link StreamTransformer}.
	 * @return the {@link StreamTransformer} instance.
	 */
	public static StreamTransformer fromStream() {
		return fromStream(null);
	}

	/**
	 * Create an instance with the charset to convert the stream to a
	 * String; if null a {@code byte[]} will be produced instead.
	 * @param charset the charset.
	 * @return the {@link StreamTransformer} instance.
	 */
	public static StreamTransformer fromStream(String charset) {
		return new StreamTransformer(charset);
	}

}
