/*
 * Copyright 2016-2024 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import reactor.util.function.Tuple2;

import org.springframework.expression.Expression;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.integration.transformer.support.AbstractHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * A {@link ConsumerEndpointSpec} extension for the {@link ContentEnricher}.
 *
 * @author Artem Bilan
 * @author Tim Ysewyn
 * @author Ian Bondoc
 * @author Alexis Hafner
 *
 * @since 5.0
 */
public class EnricherSpec extends ConsumerEndpointSpec<EnricherSpec, ContentEnricher> {

	protected final Map<String, Expression> propertyExpressions = new HashMap<>(); // NOSONAR - final

	protected final Map<String, HeaderValueMessageProcessor<?>> headerExpressions = new HashMap<>(); // NOSONAR - final

	protected EnricherSpec() {
		super(new ContentEnricher());
	}

	/**
	 * @param requestChannel the request channel.
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestChannel(MessageChannel)
	 */
	public EnricherSpec requestChannel(MessageChannel requestChannel) {
		this.handler.setRequestChannel(requestChannel);
		return _this();
	}

	/**
	 * @param requestChannel the request channel.
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestChannelName(String)
	 */
	public EnricherSpec requestChannel(String requestChannel) {
		this.handler.setRequestChannelName(requestChannel);
		return _this();
	}

	/**
	 * @param replyChannel the reply channel.
	 * @return the enricher spec.
	 * @see ContentEnricher#setReplyChannel(MessageChannel)
	 */
	public EnricherSpec replyChannel(MessageChannel replyChannel) {
		this.handler.setReplyChannel(replyChannel);
		return _this();
	}

	/**
	 * @param replyChannel the reply channel.
	 * @return the enricher spec.
	 * @see ContentEnricher#setReplyChannelName(String)
	 */
	public EnricherSpec replyChannel(String replyChannel) {
		this.handler.setReplyChannelName(replyChannel);
		return _this();
	}

	/**
	 * @param errorChannel the error channel.
	 * @return the enricher spec.
	 * @since 5.0.1
	 * @see ContentEnricher#setErrorChannel(MessageChannel)
	 */
	public EnricherSpec errorChannel(MessageChannel errorChannel) {
		this.handler.setErrorChannel(errorChannel);
		return _this();
	}

	/**
	 * @param errorChannel the name of the error channel bean.
	 * @return the enricher spec.
	 * @since 5.0.1
	 * @see ContentEnricher#setErrorChannelName(String)
	 */
	public EnricherSpec errorChannel(String errorChannel) {
		this.handler.setErrorChannelName(errorChannel);
		return _this();
	}

	/**
	 * @param requestTimeout the requestTimeout
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestTimeout(Long)
	 */
	public EnricherSpec requestTimeout(Long requestTimeout) {
		this.handler.setRequestTimeout(requestTimeout);
		return _this();
	}

	/**
	 * @param replyTimeout the replyTimeout
	 * @return the enricher spec.
	 * @see ContentEnricher#setReplyTimeout(Long)
	 */
	public EnricherSpec replyTimeout(Long replyTimeout) {
		this.handler.setReplyTimeout(replyTimeout);
		return _this();
	}

	/**
	 * @param requestPayloadExpression the requestPayloadExpression.
	 * @return the enricher spec.
	 * @see ContentEnricher#setRequestPayloadExpression(Expression)
	 */
	public EnricherSpec requestPayloadExpression(String requestPayloadExpression) {
		this.handler.setRequestPayloadExpression(PARSER.parseExpression(requestPayloadExpression));
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
		this.handler.setRequestPayloadExpression(new FunctionExpression<>(requestPayloadFunction));
		return _this();
	}

	/**
	 * The request sub-flow.
	 * @param subFlow the subFlowDefinition
	 * @return the enricher spec
	 */
	public EnricherSpec requestSubFlow(IntegrationFlow subFlow) {
		return requestChannel(obtainInputChannelFromFlow(subFlow));
	}

	/**
	 * @param shouldClonePayload the shouldClonePayload.
	 * @return the enricher spec.
	 * @see ContentEnricher#setShouldClonePayload(boolean)
	 */
	public EnricherSpec shouldClonePayload(boolean shouldClonePayload) {
		this.handler.setShouldClonePayload(shouldClonePayload);
		return _this();
	}

	/**
	 * @param key the key.
	 * @param value the value.
	 * @return the enricher spec.
	 * @see ContentEnricher#setPropertyExpressions(Map)
	 */
	public EnricherSpec property(String key, Object value) {
		this.propertyExpressions.put(key, new ValueExpression<>(value));
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
		return header(name, value, null);
	}

	/**
	 * @param name the header name.
	 * @param value the value.
	 * @param overwrite true to overwrite the header if already present.
	 * @return the enricher spec.
	 * @see ContentEnricher#setHeaderExpressions(Map)
	 */
	public EnricherSpec header(String name, Object value, @Nullable Boolean overwrite) {
		AbstractHeaderValueMessageProcessor<Object> headerValueMessageProcessor =
				new StaticHeaderValueMessageProcessor<>(value);
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
	public EnricherSpec headerExpression(String name, String expression, @Nullable Boolean overwrite) {
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
	public <P> EnricherSpec headerFunction(String name, Function<Message<P>, Object> function,
			@Nullable Boolean overwrite) {

		return headerExpression(name, new FunctionExpression<>(function), overwrite);
	}

	private EnricherSpec headerExpression(String name, Expression expression, @Nullable Boolean overwrite) {
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
	protected Tuple2<ConsumerEndpointFactoryBean, ContentEnricher> doGet() {
		if (!this.propertyExpressions.isEmpty()) {
			this.handler.setPropertyExpressions(this.propertyExpressions);
		}
		if (!this.headerExpressions.isEmpty()) {
			this.handler.setHeaderExpressions(this.headerExpressions);
		}
		return super.doGet();
	}

}
