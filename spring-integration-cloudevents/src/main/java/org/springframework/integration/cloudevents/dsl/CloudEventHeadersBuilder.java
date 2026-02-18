/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.cloudevents.dsl;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.cloudevents.CloudEventHeaders;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.MapBuilder;
import org.springframework.messaging.Message;

/**
 * The CloudEvent specific {@link MapBuilder} implementation.
 * <p>Provide a fluent API for building CloudEvent headers with support for
 * literal values, SpEL expressions, and function-based values.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class CloudEventHeadersBuilder extends MapBuilder<CloudEventHeadersBuilder, String, Object> {

	CloudEventHeadersBuilder() {
	}

	/**
	 * Set the CloudEvent id.
	 * @param id the event id
	 * @return the builder
	 */
	public CloudEventHeadersBuilder id(String id) {
		return put(CloudEventHeaders.EVENT_ID, id);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent id.
	 * @param id the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder idExpression(String id) {
		return putExpression(CloudEventHeaders.EVENT_ID, id);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent id.
	 * @param id the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder idFunction(Function<Message<P>, String> id) {
		return put(CloudEventHeaders.EVENT_ID, new FunctionExpression<>(id));
	}

	/**
	 * Set the CloudEvent source.
	 * @param source the event source URI
	 * @return the builder
	 */
	public CloudEventHeadersBuilder source(URI source) {
		return put(CloudEventHeaders.EVENT_SOURCE, source);
	}

	/**
	 * Set the CloudEvent source.
	 * @param source the event source as string
	 * @return the builder
	 */
	public CloudEventHeadersBuilder source(String source) {
		return put(CloudEventHeaders.EVENT_SOURCE, source);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent source.
	 * @param source the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder sourceExpression(String source) {
		return putExpression(CloudEventHeaders.EVENT_SOURCE, source);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent source.
	 * @param source the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder sourceFunction(Function<Message<P>, URI> source) {
		return put(CloudEventHeaders.EVENT_SOURCE, new FunctionExpression<>(source));
	}

	/**
	 * Set the CloudEvent type.
	 * @param type the event type
	 * @return the builder
	 */
	public CloudEventHeadersBuilder type(String type) {
		return put(CloudEventHeaders.EVENT_TYPE, type);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent type.
	 * @param type the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder typeExpression(String type) {
		return putExpression(CloudEventHeaders.EVENT_TYPE, type);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent type.
	 * @param type the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder typeFunction(Function<Message<P>, String> type) {
		return put(CloudEventHeaders.EVENT_TYPE, new FunctionExpression<>(type));
	}

	/**
	 * Set the CloudEvent time.
	 * @param time the event timestamp
	 * @return the builder
	 */
	@SuppressWarnings("NullAway")
	public CloudEventHeadersBuilder time(@Nullable OffsetDateTime time) {
		return put(CloudEventHeaders.EVENT_TIME, time);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent time.
	 * @param time the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder timeExpression(String time) {
		return putExpression(CloudEventHeaders.EVENT_TIME, time);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent time.
	 * @param time the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder timeFunction(Function<Message<P>, OffsetDateTime> time) {
		return put(CloudEventHeaders.EVENT_TIME, new FunctionExpression<>(time));
	}

	/**
	 * Set the CloudEvent subject.
	 * @param subject the event subject
	 * @return the builder
	 */
	@SuppressWarnings("NullAway")
	public CloudEventHeadersBuilder subject(@Nullable String subject) {
		return put(CloudEventHeaders.EVENT_SUBJECT, subject);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent subject.
	 * @param subject the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder subjectExpression(String subject) {
		return putExpression(CloudEventHeaders.EVENT_SUBJECT, subject);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent subject.
	 * @param subject the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder subjectFunction(Function<Message<P>, String> subject) {
		return put(CloudEventHeaders.EVENT_SUBJECT, new FunctionExpression<>(subject));
	}

	/**
	 * Set the CloudEvent data content type.
	 * @param dataContentType the data content type
	 * @return the builder
	 */
	@SuppressWarnings("NullAway")
	public CloudEventHeadersBuilder dataContentType(@Nullable String dataContentType) {
		return put(CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, dataContentType);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent data content type.
	 * @param dataContentType the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder dataContentTypeExpression(String dataContentType) {
		return putExpression(CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, dataContentType);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent data content type.
	 * @param dataContentType the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder dataContentTypeFunction(Function<Message<P>, String> dataContentType) {
		return put(CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, new FunctionExpression<>(dataContentType));
	}

	/**
	 * Set the CloudEvent data schema.
	 * @param dataSchema the data schema URI
	 * @return the builder
	 */
	@SuppressWarnings("NullAway")
	public CloudEventHeadersBuilder dataSchema(@Nullable URI dataSchema) {
		return put(CloudEventHeaders.EVENT_DATA_SCHEMA, dataSchema);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent data schema.
	 * @param dataSchema the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder dataSchemaExpression(String dataSchema) {
		return putExpression(CloudEventHeaders.EVENT_DATA_SCHEMA, dataSchema);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent data schema.
	 * @param dataSchema the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder dataSchemaFunction(Function<Message<P>, URI> dataSchema) {
		return put(CloudEventHeaders.EVENT_DATA_SCHEMA, new FunctionExpression<>(dataSchema));
	}

	private CloudEventHeadersBuilder putExpression(String key, String expression) {
		return put(key, PARSER.parseExpression(expression));
	}

}
