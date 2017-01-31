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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.integration.transformer.support.AbstractHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The {@link MessageHandlerSpec} implementation for the {@link ContentEnricher}.
 *
 * @author Artem Bilan
 * @author Tim Ysewyn
 *
 * @since 5.0
 */
public class EnricherSpec extends MessageHandlerSpec<EnricherSpec, ContentEnricher> {

	private final ContentEnricher enricher = new ContentEnricher();

	private final Map<String, Expression> propertyExpressions = new HashMap<String, Expression>();

	private final Map<String, HeaderValueMessageProcessor<?>> headerExpressions =
			new HashMap<String, HeaderValueMessageProcessor<?>>();

	EnricherSpec() {
		super();
	}

	/**
	 * @param requestChannel the request channel.
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestChannel(MessageChannel)
	 */
	public EnricherSpec requestChannel(MessageChannel requestChannel) {
		this.enricher.setRequestChannel(requestChannel);
		return _this();
	}

	/**
	 * @param requestChannel the request channel.
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestChannelName(String)
	 */
	public EnricherSpec requestChannel(String requestChannel) {
		this.enricher.setRequestChannelName(requestChannel);
		return _this();
	}

	/**
	 * @param replyChannel the reply channel.
	 * @return the enricher spec.
	 * @see ContentEnricher#setReplyChannel(MessageChannel)
	 */
	public EnricherSpec replyChannel(MessageChannel replyChannel) {
		this.enricher.setReplyChannel(replyChannel);
		return _this();
	}

	/**
	 * @param replyChannel the reply channel.
	 * @return the enricher spec.
	 * @see ContentEnricher#setReplyChannelName(String)
	 */
	public EnricherSpec replyChannel(String replyChannel) {
		this.enricher.setReplyChannelName(replyChannel);
		return _this();
	}

	/**
	 * @param requestTimeout the requestTimeout
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestTimeout(Long)
	 */
	public EnricherSpec requestTimeout(Long requestTimeout) {
		this.enricher.setRequestTimeout(requestTimeout);
		return _this();
	}

	/**
	 * @param replyTimeout the replyTimeout
	 * @return the enricher spec.
	 * @see ContentEnricher#setReplyTimeout(Long)
	 */
	public EnricherSpec replyTimeout(Long replyTimeout) {
		this.enricher.setReplyTimeout(replyTimeout);
		return _this();
	}

	/**
	 * @param requestPayloadExpression the requestPayloadExpression.
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestPayloadExpression(Expression)
	 */
	public EnricherSpec requestPayloadExpression(String requestPayloadExpression) {
		this.enricher.setRequestPayloadExpression(PARSER.parseExpression(requestPayloadExpression));
		return _this();
	}

	/**
	 * @param requestPayloadFunction the requestPayloadFunction.
	 * @param <P> the payload type.
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestPayloadExpression(Expression)
	 * @see FunctionExpression
	 */
	public <P> EnricherSpec requestPayload(Function<Message<P>, ?> requestPayloadFunction) {
		this.enricher.setRequestPayloadExpression(new FunctionExpression<>(requestPayloadFunction));
		return _this();
	}

	/**
	 * @param shouldClonePayload the shouldClonePayload.
	 * @return the enricher spec.
	 * @see ContentEnricher#setShouldClonePayload(boolean)
	 */
	public EnricherSpec shouldClonePayload(boolean shouldClonePayload) {
		this.enricher.setShouldClonePayload(shouldClonePayload);
		return _this();
	}

	/**
	 * @param key the key.
	 * @param value the value.
	 * @param <V> the value type.
	 * @return the enricher spec.
	 * @see ContentEnricher#setPropertyExpressions(Map)
	 */
	public <V> EnricherSpec property(String key, V value) {
		this.propertyExpressions.put(key, new ValueExpression<V>(value));
		return _this();
	}

	/**
	 * @param key the key.
	 * @param expression the expression.
	 * @return the enricher spec.
	 * @see ContentEnricher#setPropertyExpressions(Map)
	 */
	public EnricherSpec propertyExpression(String key, String expression) {
		Assert.notNull(key, "'key' must not be null");
		this.propertyExpressions.put(key, PARSER.parseExpression(expression));
		return _this();
	}

	/**
	 * @param key the key.
	 * @param function the function (usually a JDK8 lambda).
	 * @param <P> the payload type.
	 * @return the enricher spec.
	 * @see ContentEnricher#setPropertyExpressions(Map)
	 * @see FunctionExpression
	 */
	public <P> EnricherSpec propertyFunction(String key, Function<Message<P>, Object> function) {
		this.propertyExpressions.put(key, new FunctionExpression<>(function));
		return _this();
	}

	/**
	 * Set a header with the value if it is not already present.
	 * @param name the header name.
	 * @param value the value.
	 * @param <V> the value type.
	 * @return the enricher spec.
	 * @see ContentEnricher#setHeaderExpressions(Map)
	 */
	public <V> EnricherSpec header(String name, V value) {
		return this.header(name, value, null);
	}

	/**
	 * @param name the header name.
	 * @param value the value.
	 * @param overwrite true to overwrite the header if already present.
	 * @param <V> the value type.
	 * @return the enricher spec.
	 * @see ContentEnricher#setHeaderExpressions(Map)
	 */
	public <V> EnricherSpec header(String name, V value, Boolean overwrite) {
		AbstractHeaderValueMessageProcessor<V> headerValueMessageProcessor =
				new StaticHeaderValueMessageProcessor<V>(value);
		headerValueMessageProcessor.setOverwrite(overwrite);
		return header(name, headerValueMessageProcessor);
	}

	/**
	 * Set a header with the expression evaluation if the header is not already present.
	 * @param name the header name.
	 * @param expression the expression to be evaluated against the reply message to obtain the value.
	 * @return the enricher spec.
	 * @see ContentEnricher#setHeaderExpressions(Map)
	 */
	public EnricherSpec headerExpression(String name, String expression) {
		return headerExpression(name, expression, null);
	}

	/**
	 * @param name the header name.
	 * @param expression the expression to be evaluated against the reply message to obtain the value.
	 * @param overwrite true to overwrite the header if already present.
	 * @return the enricher spec.
	 * @see ContentEnricher#setHeaderExpressions(Map)
	 */
	public EnricherSpec headerExpression(String name, String expression, Boolean overwrite) {
		Assert.hasText(expression, "'expression' must not be empty");
		return headerExpression(name, PARSER.parseExpression(expression), overwrite);
	}

	/**
	 * Set a header with the function return value if the header is not already present.
	 * @param name the header name.
	 * @param function the function (usually a JDK8 lambda).
	 * @param <P> the payload type.
	 * @return the enricher spec.
	 * @see ContentEnricher#setHeaderExpressions(Map)
	 * @see FunctionExpression
	 */
	public <P> EnricherSpec headerFunction(String name, Function<Message<P>, Object> function) {
		return headerFunction(name, function, null);
	}

	/**
	 * @param name the header name.
	 * @param function the function (usually a JDK8 lambda).
	 * @param overwrite true to overwrite the header if already present.
	 * @param <P> the payload type.
	 * @return the enricher spec.
	 * @see ContentEnricher#setHeaderExpressions(Map)
	 * @see FunctionExpression
	 */
	public <P> EnricherSpec headerFunction(String name, Function<Message<P>, Object> function, Boolean overwrite) {
		return headerExpression(name, new FunctionExpression<>(function), overwrite);
	}

	private EnricherSpec headerExpression(String name, Expression expression, Boolean overwrite) {
		AbstractHeaderValueMessageProcessor<?> headerValueMessageProcessor =
				new ExpressionEvaluatingHeaderValueMessageProcessor<>(expression, null);
		headerValueMessageProcessor.setOverwrite(overwrite);
		return header(name, headerValueMessageProcessor);
	}

	/**
	 * Set a header value using an explicit {@link HeaderValueMessageProcessor}.
	 * @param headerName the header name.
	 * @param headerValueMessageProcessor the headerValueMessageProcessor.
	 * @param <V> the value type.
	 * @return the enricher spec.
	 * @see ContentEnricher#setHeaderExpressions(Map)
	 */
	public <V> EnricherSpec header(String headerName, HeaderValueMessageProcessor<V> headerValueMessageProcessor) {
		Assert.hasText(headerName, "'headerName' must not be empty");
		this.headerExpressions.put(headerName, headerValueMessageProcessor);
		return _this();
	}

	@Override
	protected ContentEnricher doGet() {
		if (!this.propertyExpressions.isEmpty()) {
			this.enricher.setPropertyExpressions(this.propertyExpressions);
		}
		if (!this.headerExpressions.isEmpty()) {
			this.enricher.setHeaderExpressions(this.headerExpressions);
		}
		return this.enricher;
	}

}
