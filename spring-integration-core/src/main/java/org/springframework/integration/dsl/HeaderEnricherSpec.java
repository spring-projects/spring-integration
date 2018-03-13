/*
 * Copyright 2016-2018 the original author or authors.
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
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.handler.BeanNameMessageProcessor;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.MapBuilder;
import org.springframework.integration.support.StringStringMapBuilder;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.support.AbstractHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import reactor.util.function.Tuple2;

/**
 * An {@link IntegrationComponentSpec} for a {@link HeaderEnricher}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class HeaderEnricherSpec extends ConsumerEndpointSpec<HeaderEnricherSpec, MessageTransformingHandler> {

	private final Map<String, HeaderValueMessageProcessor<?>> headerToAdd = new HashMap<>();

	private final HeaderEnricher headerEnricher = new HeaderEnricher(this.headerToAdd);

	HeaderEnricherSpec() {
		super(null);
		this.handler = new MessageTransformingHandler(this.headerEnricher);
	}

	/**
	 * Determine the default action to take when setting individual header specifications
	 * without an explicit 'overwrite' argument.
	 * @param defaultOverwrite the defaultOverwrite.
	 * @return the header enricher spec.
	 * @see HeaderEnricher#setDefaultOverwrite(boolean)
	 */
	public HeaderEnricherSpec defaultOverwrite(boolean defaultOverwrite) {
		this.headerEnricher.setDefaultOverwrite(defaultOverwrite);
		return _this();
	}

	/**
	 * @param shouldSkipNulls the shouldSkipNulls.
	 * @return the header enricher spec.
	 * @see HeaderEnricher#setShouldSkipNulls(boolean)
	 */
	public HeaderEnricherSpec shouldSkipNulls(boolean shouldSkipNulls) {
		this.headerEnricher.setShouldSkipNulls(shouldSkipNulls);
		return _this();
	}

	/**
	 * Configure an optional custom {@link MessageProcessor} for the enricher. The
	 * processor must return a {@link Map} of header names and values. They will be added
	 * to the inbound message headers before evaluating the individual configured header
	 * specifications.
	 * @param messageProcessor the messageProcessor.
	 * @return the header enricher spec.
	 * @see HeaderEnricher#setMessageProcessor(MessageProcessor)
	 */
	public HeaderEnricherSpec messageProcessor(MessageProcessor<?> messageProcessor) {
		this.headerEnricher.setMessageProcessor(messageProcessor);
		return _this();
	}

	/**
	 * Configure an {@link ExpressionEvaluatingMessageProcessor} that evaluates to a
	 * {@link Map} of additional headers. They will be added to the inbound message
	 * headers before evaluating the individual configured header specifications.
	 * @param expression the expression.
	 * @return the header enricher spec.
	 * @see #messageProcessor(MessageProcessor)
	 */
	public HeaderEnricherSpec messageProcessor(String expression) {
		return messageProcessor(new ExpressionEvaluatingMessageProcessor<>(expression));
	}

	/**
	 * Configure an
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor} that
	 * invokes the method on the bean - the method must return a {@link Map} of headers.
	 * They will be added to the inbound message headers before evaluating the individual
	 * configured header specifications.
	 * @param beanName The bean name.
	 * @param methodName The method name.
	 * @return the header enricher spec.
	 * @see #messageProcessor(MessageProcessor)
	 */
	public HeaderEnricherSpec messageProcessor(String beanName, String methodName) {
		return messageProcessor(new BeanNameMessageProcessor<>(beanName, methodName));
	}

	/**
	 * Add header specifications from the {@link MapBuilder}; if a map value is an
	 * {@link Expression}, it will be evaluated at run time when the message headers are
	 * enriched. Otherwise the value is simply added to the headers. Headers derived from
	 * the map will <b>not</b> overwrite existing headers, unless
	 * {@link #defaultOverwrite(boolean)} is true.
	 * @param headers the header map builder.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headers(MapBuilder<?, String, Object> headers) {
		return headers(headers, null);
	}

	/**
	 * Add header specifications from the {@link MapBuilder}; if a map value is an
	 * {@link Expression}, it will be evaluated at run time when the message headers are
	 * enriched. Otherwise the value is simply added to the headers.
	 * @param headers the header map builder.
	 * @param overwrite true to overwrite existing headers.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headers(MapBuilder<?, String, Object> headers, Boolean overwrite) {
		Assert.notNull(headers, "'headers' must not be null");
		return headers(headers.get(), overwrite);
	}

	/**
	 * Add header specifications from the {@link Map}; if a map value is an
	 * {@link Expression}, it will be evaluated at run time when the message headers are
	 * enriched. Otherwise the value is simply added to the headers. Headers derived from
	 * the map will <em>not</em> overwrite existing headers, unless
	 * {@link #defaultOverwrite(boolean)} is true.
	 * @param headers The header builder.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headers(Map<String, Object> headers) {
		return headers(headers, null);
	}

	/**
	 * Add header specifications from the {@link Map}; if a map value is an
	 * {@link Expression}, it will be evaluated at run time when the message headers are
	 * enriched. Otherwise the value is simply added to the headers.
	 * @param headers The header builder.
	 * @param overwrite true to overwrite existing headers.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headers(Map<String, Object> headers, Boolean overwrite) {
		Assert.notNull(headers, "'headers' must not be null");
		for (Entry<String, Object> entry : headers.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Expression) {
				AbstractHeaderValueMessageProcessor<Object> processor =
						new ExpressionEvaluatingHeaderValueMessageProcessor<>((Expression) value, null);
				processor.setOverwrite(overwrite);
				header(name, processor);
			}
			else {
				header(name, value, overwrite);
			}
		}
		return this;
	}

	/**
	 * Add header specifications from the {@link MapBuilder}; the {@link Map} values must
	 * be String representations of SpEL expressions that will be evaluated at run time
	 * when the message headers are enriched. Headers derived from the map will <b>not</b>
	 * overwrite existing headers, unless {@link #defaultOverwrite(boolean)} is true.
	 * @param headers the header map builder.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headerExpressions(MapBuilder<?, String, String> headers) {
		return headerExpressions(headers, null);
	}

	/**
	 * Add header specifications from the {@link MapBuilder}; the {@link Map} values must
	 * be String representations of SpEL expressions that will be evaluated at run time
	 * when the message headers are enriched.
	 * @param headers the header map builder.
	 * @param overwrite true to overwrite existing headers.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headerExpressions(MapBuilder<?, String, String> headers, Boolean overwrite) {
		Assert.notNull(headers, "'headers' must not be null");
		return headerExpressions(headers.get(), overwrite);
	}

	/**
	 * Add header specifications via the consumer callback, which receives a
	 * {@link StringStringMapBuilder}; the {@link Map} values must be String
	 * representations of SpEL expressions that will be evaluated at run time when the
	 * message headers are enriched. Headers derived from the map will <b>not</b>
	 * overwrite existing headers, unless {@link #defaultOverwrite(boolean)} is true.
	 * Usually used with a JDK8 lambda:
	 * <pre class="code">
	 * {@code
	 * .enrichHeaders(s -> s.headerExpressions(c -> c
	 * 			.put(MailHeaders.SUBJECT, "payload.subject")
	 * 			.put(MailHeaders.FROM,    "payload.from[0].toString()")))
	 * }
	 * </pre>
	 * @param configurer the configurer.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headerExpressions(Consumer<StringStringMapBuilder> configurer) {
		return headerExpressions(configurer, null);
	}

	/**
	 * Add header specifications via the consumer callback, which receives a
	 * {@link StringStringMapBuilder}; the {@link Map} values must be String
	 * representations of SpEL expressions that will be evaluated at run time when the
	 * message headers are enriched. Usually used with a JDK8 lambda:
	 * <pre class="code">
	 * {@code
	 * .enrichHeaders(s -> s.headerExpressions(c -> c
	 * 			.put(MailHeaders.SUBJECT, "payload.subject")
	 * 			.put(MailHeaders.FROM,    "payload.from[0].toString()"), true))
	 * }
	 * </pre>
	 * @param configurer the configurer.
	 * @param overwrite true to overwrite existing headers.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headerExpressions(Consumer<StringStringMapBuilder> configurer, Boolean overwrite) {
		Assert.notNull(configurer, "'configurer' must not be null");
		StringStringMapBuilder builder = new StringStringMapBuilder();
		configurer.accept(builder);
		return headerExpressions(builder.get(), overwrite);
	}

	/**
	 * Add header specifications; the {@link Map} values must be String representations
	 * of SpEL expressions that will be evaluated at run time when the message headers are
	 * enriched. Headers derived from the map will <b>not</b> overwrite existing headers,
	 * unless {@link #defaultOverwrite(boolean)} is true.
	 * @param headers the headers.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headerExpressions(Map<String, String> headers) {
		return headerExpressions(headers, null);
	}

	/**
	 * Add header specifications; the {@link Map} values must be String representations of
	 * SpEL expressions that will be evaluated at run time when the message headers are
	 * enriched.
	 * @param headers the headers.
	 * @param overwrite true to overwrite existing headers.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headerExpressions(Map<String, String> headers, Boolean overwrite) {
		Assert.notNull(headers, "'headers' must not be null");
		for (Entry<String, String> entry : headers.entrySet()) {
			AbstractHeaderValueMessageProcessor<Object> processor =
					new ExpressionEvaluatingHeaderValueMessageProcessor<>(entry.getValue(), null);
			processor.setOverwrite(overwrite);
			header(entry.getKey(), processor);
		}
		return this;
	}

	/**
	 * Add a single header specification. If the header exists, it will <b>not</b> be
	 * overwritten unless {@link #defaultOverwrite(boolean)} is true.
	 * @param name the header name.
	 * @param value the header value (not an {@link Expression}).
	 * @param <V> the value type.
	 * @return the header enricher spec.
	 */
	public <V> HeaderEnricherSpec header(String name, V value) {
		return header(name, value, null);
	}

	/**
	 * Add a single header specification.
	 * @param name the header name.
	 * @param value the header value (not an {@link Expression}).
	 * @param overwrite true to overwrite an existing header.
	 * @param <V> the value type.
	 * @return the header enricher spec.
	 */
	public <V> HeaderEnricherSpec header(String name, V value, Boolean overwrite) {
		AbstractHeaderValueMessageProcessor<V> headerValueMessageProcessor =
				new StaticHeaderValueMessageProcessor<>(value);
		headerValueMessageProcessor.setOverwrite(overwrite);
		return header(name, headerValueMessageProcessor);
	}

	/**
	 * Add a single header specification where the value is a String representation of a
	 * SpEL {@link Expression}. If the header exists, it will <b>not</b> be overwritten
	 * unless {@link #defaultOverwrite(boolean)} is true.
	 * @param name the header name.
	 * @param expression the expression.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headerExpression(String name, String expression) {
		return headerExpression(name, expression, null);
	}

	/**
	 * Add a single header specification where the value is a String representation of a
	 * SpEL {@link Expression}.
	 * @param name the header name.
	 * @param expression the expression.
	 * @param overwrite true to overwrite an existing header.
	 * @return the header enricher spec.
	 */
	public HeaderEnricherSpec headerExpression(String name, String expression, Boolean overwrite) {
		Assert.hasText(expression, "'expression' must not be empty");
		return headerExpression(name, PARSER.parseExpression(expression), overwrite);
	}

	/**
	 * Add a single header specification where the value is obtained by invoking the
	 * {@link Function} callback. If the header exists, it will <b>not</b> be overwritten
	 * unless {@link #defaultOverwrite(boolean)} is true.
	 * @param name the header name.
	 * @param function the function.
	 * @param <P> the payload type.
	 * @return the header enricher spec.
	 * @see FunctionExpression
	 */
	public <P> HeaderEnricherSpec headerFunction(String name, Function<Message<P>, Object> function) {
		return headerFunction(name, function, null);
	}

	/**
	 * Add a single header specification where the value is obtained by invoking the
	 * {@link Function} callback.
	 * @param name the header name.
	 * @param function the function.
	 * @param overwrite true to overwrite an existing header.
	 * @param <P> the payload type.
	 * @return the header enricher spec.
	 * @see FunctionExpression
	 */
	public <P> HeaderEnricherSpec headerFunction(String name, Function<Message<P>, Object> function,
			Boolean overwrite) {

		return headerExpression(name, new FunctionExpression<>(function), overwrite);
	}

	private HeaderEnricherSpec headerExpression(String name, Expression expression, Boolean overwrite) {
		AbstractHeaderValueMessageProcessor<?> headerValueMessageProcessor =
				new ExpressionEvaluatingHeaderValueMessageProcessor<>(expression, null);
		headerValueMessageProcessor.setOverwrite(overwrite);
		return header(name, headerValueMessageProcessor);
	}

	/**
	 * Add a single header specification where the value is obtained by calling the
	 * {@link HeaderValueMessageProcessor}.
	 * @param headerName the header name.
	 * @param headerValueMessageProcessor the message processor.
	 * @param <V> the value type.
	 * @return the header enricher spec.
	 */
	public <V> HeaderEnricherSpec header(String headerName,
			HeaderValueMessageProcessor<V> headerValueMessageProcessor) {

		Assert.hasText(headerName, "'headerName' must not be empty");
		this.headerToAdd.put(headerName, headerValueMessageProcessor);
		return _this();
	}

	/**
	 * Add header specifications to automatically convert header channels (reply, error
	 * channels) to Strings and store them in a header channel registry. Allows
	 * persistence and serialization of messages without losing these important framework
	 * headers.
	 * @return the header enricher spec.
	 * @see org.springframework.integration.support.channel.HeaderChannelRegistry
	 */
	public HeaderEnricherSpec headerChannelsToString() {
		return headerChannelsToString(null);
	}

	/**
	 * Add header specifications to automatically convert header channels (reply, error
	 * channels) to Strings and store them in a header channel registry. Allows
	 * persistence and serialization of messages without losing these important framework
	 * headers.
	 * @param timeToLiveExpression the minimum time that the mapping will remain in the registry.
	 * @return the header enricher spec.
	 * @see org.springframework.integration.support.channel.HeaderChannelRegistry
	 */
	public HeaderEnricherSpec headerChannelsToString(String timeToLiveExpression) {
		return headerExpression("replyChannel",
				"@" + IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME
						+ ".channelToChannelName(headers.replyChannel" +
						(StringUtils.hasText(timeToLiveExpression) ? ", " + timeToLiveExpression : "") + ")",
				true)
				.headerExpression("errorChannel",
						"@" + IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME
								+ ".channelToChannelName(headers.errorChannel" +
								(StringUtils.hasText(timeToLiveExpression) ? ", " + timeToLiveExpression : "") + ")",
						true);
	}

	@Override
	protected Tuple2<ConsumerEndpointFactoryBean, MessageTransformingHandler> doGet() {
		this.componentsToRegister.put(this.headerEnricher, null);
		return super.doGet();
	}

}
