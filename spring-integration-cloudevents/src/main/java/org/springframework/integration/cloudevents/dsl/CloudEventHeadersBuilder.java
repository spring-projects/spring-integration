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
 * <p>
 * Provide a fluent API for building CloudEvent headers with support for
 * literal values, SpEL expressions, and function-based values.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class CloudEventHeadersBuilder extends MapBuilder<CloudEventHeadersBuilder, String, Object> {

	private String prefix = CloudEventHeaders.PREFIX;

	private String eventIdKey = CloudEventHeaders.EVENT_ID;

	private String eventTypeKey = CloudEventHeaders.EVENT_TYPE;

	private String eventSourceKey = CloudEventHeaders.EVENT_SOURCE;

	private String eventSubjectKey = CloudEventHeaders.EVENT_SUBJECT;

	private String eventTimeKey = CloudEventHeaders.EVENT_TIME;

	private String eventDataContentTypeKey = CloudEventHeaders.EVENT_DATA_CONTENT_TYPE;

	private String eventDataSchemaKey = CloudEventHeaders.EVENT_DATA_SCHEMA;

	/**
	 * Create a new {@link CloudEventHeadersBuilder}.
	 */
	public CloudEventHeadersBuilder() {
	}

	/**
	 * Create a new {@link CloudEventHeadersBuilder} with the given prefix.
	 * @param prefix the CloudEvent header prefix
	 */
	public CloudEventHeadersBuilder(String prefix) {
		this.prefix = prefix;
		updatePrefix();
	}

	/**
	 * Set the CloudEvent id.
	 * @param id the event id
	 * @return the builder
	 */
	public CloudEventHeadersBuilder id(String id) {
		return put(this.eventIdKey, id);
	}

	/**
	 * Set the CloudEvent prefix.
	 * @param prefix the event prefix
	 * @return the builder
	 */
	public CloudEventHeadersBuilder prefix(String prefix) {
		this.prefix = prefix;
		updatePrefix();
		return _this();
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent id.
	 * @param id the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder idExpression(String id) {
		return putExpression(this.eventIdKey, id);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent id.
	 * @param id the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder idFunction(Function<Message<P>, String> id) {
		return put(this.eventIdKey, new FunctionExpression<>(id));
	}

	/**
	 * Set the CloudEvent source.
	 * @param source the event source URI
	 * @return the builder
	 */
	public CloudEventHeadersBuilder source(URI source) {
		return put(this.eventSourceKey, source);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent source.
	 * @param source the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder sourceExpression(String source) {
		return putExpression(this.eventSourceKey, source);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent source.
	 * @param source the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder sourceFunction(Function<Message<P>, URI> source) {
		return put(this.eventSourceKey, new FunctionExpression<>(source));
	}

	/**
	 * Set the CloudEvent type.
	 * @param type the event type
	 * @return the builder
	 */
	public CloudEventHeadersBuilder type(String type) {
		return put(this.eventTypeKey, type);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent type.
	 * @param type the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder typeExpression(String type) {
		return putExpression(this.eventTypeKey, type);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent type.
	 * @param type the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder typeFunction(Function<Message<P>, String> type) {
		return put(this.eventTypeKey, new FunctionExpression<>(type));
	}

	/**
	 * Set the CloudEvent time.
	 * @param time the event timestamp
	 * @return the builder
	 */
	@SuppressWarnings("NullAway")
	public CloudEventHeadersBuilder time(@Nullable OffsetDateTime time) {
		return put(this.eventTimeKey, time);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent time.
	 * @param time the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder timeExpression(String time) {
		return putExpression(this.eventTimeKey, time);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent time.
	 * @param time the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder timeFunction(Function<Message<P>, OffsetDateTime> time) {
		return put(this.eventTimeKey, new FunctionExpression<>(time));
	}

	/**
	 * Set the CloudEvent subject.
	 * @param subject the event subject
	 * @return the builder
	 */
	@SuppressWarnings("NullAway")
	public CloudEventHeadersBuilder subject(@Nullable String subject) {
		return put(this.eventSubjectKey, subject);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent subject.
	 * @param subject the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder subjectExpression(String subject) {
		return putExpression(this.eventSubjectKey, subject);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent subject.
	 * @param subject the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder subjectFunction(Function<Message<P>, String> subject) {
		return put(this.eventSubjectKey, new FunctionExpression<>(subject));
	}

	/**
	 * Set the CloudEvent data content type.
	 * @param dataContentType the data content type
	 * @return the builder
	 */
	@SuppressWarnings("NullAway")
	public CloudEventHeadersBuilder dataContentType(@Nullable String dataContentType) {
		return put(this.eventDataContentTypeKey, dataContentType);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent data content type.
	 * @param dataContentType the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder dataContentTypeExpression(String dataContentType) {
		return putExpression(this.eventDataContentTypeKey, dataContentType);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent data content type.
	 * @param dataContentType the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder dataContentTypeFunction(Function<Message<P>, String> dataContentType) {
		return put(this.eventDataContentTypeKey, new FunctionExpression<>(dataContentType));
	}

	/**
	 * Set the CloudEvent data schema.
	 * @param dataSchema the data schema URI
	 * @return the builder
	 */
	@SuppressWarnings("NullAway")
	public CloudEventHeadersBuilder dataSchema(@Nullable URI dataSchema) {
		return put(this.eventDataSchemaKey, dataSchema);
	}

	/**
	 * Set the expression that will be evaluated to determine the CloudEvent data schema.
	 * @param dataSchema the SpEL expression
	 * @return the builder
	 */
	public CloudEventHeadersBuilder dataSchemaExpression(String dataSchema) {
		return putExpression(this.eventDataSchemaKey, dataSchema);
	}

	/**
	 * Set a function that will be invoked to determine the CloudEvent data schema.
	 * @param dataSchema the function
	 * @return the builder
	 */
	public <P> CloudEventHeadersBuilder dataSchemaFunction(Function<Message<P>, URI> dataSchema) {
		return put(this.eventDataSchemaKey, new FunctionExpression<>(dataSchema));
	}

	private CloudEventHeadersBuilder putExpression(String key, String expression) {
		return put(key, PARSER.parseExpression(expression));
	}

	private void updatePrefix() {
		this.eventIdKey = this.prefix + "id";
		this.eventTimeKey = this.prefix + "time";
		this.eventTypeKey = this.prefix + "type";
		this.eventDataContentTypeKey = this.prefix + "datacontenttype";
		this.eventDataSchemaKey = this.prefix + "dataschema";
		this.eventSourceKey = this.prefix + "source";
		this.eventSubjectKey = this.prefix + "subject";
	}

}

