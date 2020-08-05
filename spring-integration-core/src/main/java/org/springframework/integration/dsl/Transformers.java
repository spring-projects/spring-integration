/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.MessageBuilder;
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
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * An utility class to provide methods for out-of-the-box
 * {@link org.springframework.integration.transformer.Transformer}s.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class Transformers {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	public static ObjectToStringTransformer objectToString() {
		return objectToString(null);
	}

	public static ObjectToStringTransformer objectToString(@Nullable String charset) {
		return charset != null
				? new ObjectToStringTransformer(charset)
				: new ObjectToStringTransformer();
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
		ObjectToMapTransformer transformer = new ObjectToMapTransformer(jsonObjectMapper);
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

	public static ObjectToJsonTransformer toJson(@Nullable JsonObjectMapper<?, ?> jsonObjectMapper) {
		return toJson(jsonObjectMapper, null, null);
	}

	public static ObjectToJsonTransformer toJson(@Nullable JsonObjectMapper<?, ?> jsonObjectMapper,
			@Nullable ObjectToJsonTransformer.ResultType resultType) {
		return toJson(jsonObjectMapper, resultType, null);
	}

	public static ObjectToJsonTransformer toJson(@Nullable String contentType) {
		return toJson(null, null, contentType);
	}

	public static ObjectToJsonTransformer toJson(@Nullable JsonObjectMapper<?, ?> jsonObjectMapper,
			@Nullable String contentType) {
		return toJson(jsonObjectMapper, null, contentType);
	}

	/**
	 * Factory for the {@link ObjectToJsonTransformer} based on the provided {@link ObjectToJsonTransformer.ResultType}.
	 * @param resultType the {@link ObjectToJsonTransformer.ResultType} to use.
	 * Defaults to {@link ObjectToJsonTransformer.ResultType#STRING}.
	 * @return the ObjectToJsonTransformer
	 * @since 5.0.9
	 */
	public static ObjectToJsonTransformer toJson(@Nullable ObjectToJsonTransformer.ResultType resultType) {
		return toJson(null, resultType, null);
	}

	public static ObjectToJsonTransformer toJson(@Nullable ObjectToJsonTransformer.ResultType resultType,
			@Nullable String contentType) {
		return toJson(null, resultType, contentType);
	}

	public static ObjectToJsonTransformer toJson(@Nullable JsonObjectMapper<?, ?> jsonObjectMapper,
			@Nullable ObjectToJsonTransformer.ResultType resultType, @Nullable String contentType) {

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
		return fromJson((Class<?>) null, null);
	}

	public static JsonToObjectTransformer fromJson(@Nullable Class<?> targetClass) {
		return fromJson(targetClass, null);
	}

	/**
	 * Construct a {@link JsonToObjectTransformer} based on the provided {@link ResolvableType}.
	 * @param targetType the {@link ResolvableType} top use.
	 * @return the {@link JsonToObjectTransformer} instance.
	 * @since 5.2
	 */
	public static JsonToObjectTransformer fromJson(ResolvableType targetType) {
		return fromJson(targetType, null);
	}

	public static JsonToObjectTransformer fromJson(@Nullable JsonObjectMapper<?, ?> jsonObjectMapper) {
		return fromJson((Class<?>) null, jsonObjectMapper);
	}

	public static JsonToObjectTransformer fromJson(@Nullable Class<?> targetClass,
			@Nullable JsonObjectMapper<?, ?> jsonObjectMapper) {

		return new JsonToObjectTransformer(targetClass, jsonObjectMapper);
	}

	/**
	 * Construct a {@link JsonToObjectTransformer} based on the provided {@link ResolvableType}
	 * and {@link JsonObjectMapper}.
	 * @param targetType the {@link ResolvableType} top use.
	 * @param jsonObjectMapper the {@link JsonObjectMapper} top use.
	 * @return the {@link JsonToObjectTransformer} instance.
	 * @since 5.2
	 */
	public static JsonToObjectTransformer fromJson(ResolvableType targetType,
			@Nullable JsonObjectMapper<?, ?> jsonObjectMapper) {

		return new JsonToObjectTransformer(targetType, jsonObjectMapper);
	}

	public static PayloadSerializingTransformer serializer() {
		return serializer(null);
	}

	public static PayloadSerializingTransformer serializer(@Nullable Serializer<Object> serializer) {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		if (serializer != null) {
			transformer.setSerializer(serializer);
		}
		return transformer;
	}

	public static PayloadDeserializingTransformer deserializer(String... allowedPatterns) {
		return deserializer(null, allowedPatterns);
	}

	public static PayloadDeserializingTransformer deserializer(@Nullable Deserializer<Object> deserializer,
			String... allowedPatterns) {

		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.setAllowedPatterns(allowedPatterns);
		if (deserializer != null) {
			transformer.setDeserializer(deserializer);
		}
		return transformer;
	}

	public static <T, U> PayloadTypeConvertingTransformer<T, U> converter(Converter<T, U> converter) {
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
	public static StreamTransformer fromStream(@Nullable String charset) {
		return new StreamTransformer(charset);
	}


	@SuppressWarnings("unchecked")
	static <I, O> Flux<Message<O>> transformWithFunction(Publisher<Message<I>> publisher,
			Function<? super Flux<Message<I>>, ? extends Publisher<O>> fluxFunction) {

		return Flux.from(publisher)
				.flatMap(message ->
						Mono.deferContextual(ctx -> {
							ctx.get(RequestMessageHolder.class).set(message);
							return Mono.just(message);
						}))
				.transform(fluxFunction)
				.flatMap(data ->
						data instanceof Message<?>
								? Mono.just((Message<O>) data)
								: Mono.deferContextual(ctx -> Mono.just(ctx.get(RequestMessageHolder.class).get()))
								.map(requestMessage ->
										MessageBuilder.withPayload(data)
												.copyHeaders(requestMessage.getHeaders())
												.build()))
				.contextWrite(ctx -> ctx.put(RequestMessageHolder.class, new RequestMessageHolder()));
	}


	@SuppressWarnings("serial")
	private static class RequestMessageHolder extends AtomicReference<Message<?>> {

	}

}
