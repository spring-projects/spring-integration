/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.gateway.AnnotationGatewayProxyFactoryBean;
import org.springframework.integration.gateway.GatewayMethodMetadata;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.MethodArgsHolder;
import org.springframework.integration.gateway.MethodArgsMessageMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;

/**
 * A builder for the {@link GatewayProxyFactoryBean} options
 * when {@link org.springframework.integration.annotation.MessagingGateway} on the service interface cannot be
 * declared.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 5.2
 */
public class GatewayProxySpec implements ComponentsRegistration {

	protected  static final SpelExpressionParser PARSER = new SpelExpressionParser(); // NOSONAR - final

	protected final MessageChannel gatewayRequestChannel = new DirectChannel(); // NOSONAR - final

	protected final GatewayProxyFactoryBean gatewayProxyFactoryBean; // NOSONAR - final

	protected final GatewayMethodMetadata gatewayMethodMetadata = new GatewayMethodMetadata(); // NOSONAR - final

	protected final Map<String, Expression> headerExpressions = new HashMap<>(); // NOSONAR - final

	private boolean populateGatewayMethodMetadata;

	protected GatewayProxySpec(Class<?> serviceInterface) {
		this.gatewayProxyFactoryBean = new AnnotationGatewayProxyFactoryBean(serviceInterface);
		this.gatewayProxyFactoryBean.setDefaultRequestChannel(this.gatewayRequestChannel);
	}

	/**
	 * Specify a bean name for the target {@link GatewayProxyFactoryBean}.
	 * @param beanName the bean name to be used for registering bean for the gateway proxy
	 * @return current {@link GatewayProxySpec}.
	 */
	public GatewayProxySpec beanName(@Nullable String beanName) {
		if (beanName != null) {
			this.gatewayProxyFactoryBean.setBeanName(beanName);
		}
		return this;
	}

	/**
	 * Identifies the default channel the gateway proxy will subscribe to, to receive reply
	 * {@code Message}s, the payloads of
	 * which will be converted to the return type of the method signature.
	 * @param channelName the bean name for {@link MessageChannel}
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setDefaultReplyChannel
	 */
	public GatewayProxySpec replyChannel(String channelName) {
		this.gatewayProxyFactoryBean.setDefaultReplyChannelName(channelName);
		return this;
	}

	/**
	 * Identifies the default channel the gateway proxy will subscribe to, to receive reply
	 * {@code Message}s, the payloads of
	 * which will be converted to the return type of the method signature.
	 * @param replyChannel the {@link MessageChannel} for replies.
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setDefaultReplyChannel
	 */
	public GatewayProxySpec replyChannel(MessageChannel replyChannel) {
		this.gatewayProxyFactoryBean.setDefaultReplyChannel(replyChannel);
		return this;
	}

	/**
	 * Identifies a channel that error messages will be sent to if a failure occurs in the
	 * gateway's proxy invocation. If no {@code errorChannel} reference is provided, the gateway will
	 * propagate {@code Exception}s to the caller. To completely suppress {@code Exception}s, provide a
	 * reference to the {@code nullChannel} here.
	 * @param errorChannelName the bean name for {@link MessageChannel}
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setErrorChannel
	 */
	public GatewayProxySpec errorChannel(String errorChannelName) {
		this.gatewayProxyFactoryBean.setErrorChannelName(errorChannelName);
		return this;
	}

	/**
	 * Identifies a channel that error messages will be sent to if a failure occurs in the
	 * gateway's proxy invocation. If no {@code errorChannel} reference is provided, the gateway will
	 * propagate {@code Exception}s to the caller. To completely suppress {@code Exception}s, provide a
	 * reference to the {@code nullChannel} here.
	 * @param errorChannel the {@link MessageChannel} for replies.
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setErrorChannel
	 */
	public GatewayProxySpec errorChannel(MessageChannel errorChannel) {
		this.gatewayProxyFactoryBean.setErrorChannel(errorChannel);
		return this;
	}

	/**
	 * Provides the amount of time dispatcher would wait to send a {@code Message}. This
	 * timeout would only apply if there is a potential to block in the send call. For
	 * example if this gateway is hooked up to a {@code QueueChannel}. Value is specified
	 * in milliseconds.
	 * @param requestTimeout the timeout for requests in milliseconds.
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setDefaultRequestTimeout
	 */
	public GatewayProxySpec requestTimeout(long requestTimeout) {
		this.gatewayProxyFactoryBean.setDefaultRequestTimeout(requestTimeout);
		return this;
	}

	/**
	 * Allows to specify how long this gateway will wait for the reply {@code Message}
	 * before returning. By default it will wait indefinitely. {@code null} is returned if
	 * the gateway times out. Value is specified in milliseconds.
	 * @param replyTimeout the timeout for replies in milliseconds.
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setDefaultReplyTimeout
	 */
	public GatewayProxySpec replyTimeout(long replyTimeout) {
		this.gatewayProxyFactoryBean.setDefaultReplyTimeout(replyTimeout);
		return this;
	}

	/**
	 * Provide a reference to an implementation of {@link Executor}
	 * to use for any of the interface methods that have a {@link java.util.concurrent.Future} return type.
	 * This {@code Executor} will only be used for those async methods; the sync methods
	 * will be invoked in the caller's thread.
	 * Use {@code null} to specify no async executor - for example
	 * if your downstream flow returns a {@link java.util.concurrent.Future}.
	 * @param executor the {@link Executor} to use.
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setAsyncExecutor
	 */
	public GatewayProxySpec asyncExecutor(@Nullable Executor executor) {
		this.gatewayProxyFactoryBean.setAsyncExecutor(executor);
		return this;
	}

	/**
	 * An expression that will be used to generate the {@code payload} for all methods in the service interface
	 * unless explicitly overridden by a method declaration.
	 * The root object for evaluation context is {@link MethodArgsHolder}.
	 * @param expression the SpEL expression for default payload.
	 * @return current {@link GatewayProxySpec}.
	 * @see org.springframework.integration.annotation.MessagingGateway#defaultPayloadExpression
	 */
	public GatewayProxySpec payloadExpression(String expression) {
		return payloadExpression(PARSER.parseExpression(expression));
	}

	/**
	 * A {@link Function} that will be used to generate the {@code payload} for all methods in the service interface
	 * unless explicitly overridden by a method declaration.
	 * @param defaultPayloadFunction the {@link Function} for default payload.
	 * @return current {@link GatewayProxySpec}.
	 * @see org.springframework.integration.annotation.MessagingGateway#defaultPayloadExpression
	 */
	public GatewayProxySpec payloadFunction(Function<MethodArgsHolder, ?> defaultPayloadFunction) {
		return payloadExpression(new FunctionExpression<>(defaultPayloadFunction));
	}

	/**
	 * An expression that will be used to generate the {@code payload} for all methods in the service interface
	 * unless explicitly overridden by a method declaration.
	 * The root object for evaluation context is {@link MethodArgsHolder}.
	 * a bean resolver is also available, enabling expressions like {@code @someBean(#args)}.
	 * @param expression the SpEL expression for default payload.
	 * @return current {@link GatewayProxySpec}.
	 * @see org.springframework.integration.annotation.MessagingGateway#defaultPayloadExpression
	 */
	public GatewayProxySpec payloadExpression(Expression expression) {
		this.gatewayMethodMetadata.setPayloadExpression(expression);
		this.populateGatewayMethodMetadata = true;
		return this;
	}

	/**
	 * Provides custom message header. The default headers are created for
	 * all methods on the service-interface (unless overridden by a specific method).
	 * @param headerName the name ofr the header.
	 * @param value the static value for the header.
	 * @return current {@link GatewayProxySpec}.
	 * @see org.springframework.integration.annotation.MessagingGateway#defaultHeaders
	 */
	public GatewayProxySpec header(String headerName, Object value) {
		return header(headerName, new ValueExpression<>(value));
	}

	/**
	 * Provides custom message header. The default headers are created for
	 * all methods on the service-interface (unless overridden by a specific method).
	 * @param headerName the name ofr the header.
	 * @param valueFunction the  {@link Function} for the header value.
	 * @return current {@link GatewayProxySpec}.
	 * @see org.springframework.integration.annotation.MessagingGateway#defaultHeaders
	 */
	public GatewayProxySpec header(String headerName, Function<MethodArgsHolder, ?> valueFunction) {
		return header(headerName, new FunctionExpression<>(valueFunction));
	}

	/**
	 * Provides custom message header. The default headers are created for
	 * all methods on the service-interface (unless overridden by a specific method).
	 * This expression-based header can get access to the {@link MethodArgsHolder}
	 * as a root object for evaluation context.
	 * @param headerName the name ofr the header.
	 * @param valueExpression the SpEL expression for the header value.
	 * @return current {@link GatewayProxySpec}.
	 * @see org.springframework.integration.annotation.MessagingGateway#defaultHeaders
	 */
	public GatewayProxySpec header(String headerName, Expression valueExpression) {
		this.headerExpressions.put(headerName, valueExpression);
		this.populateGatewayMethodMetadata = true;
		return this;
	}

	/**
	 * An {@link MethodArgsMessageMapper}
	 * to map the method arguments to a {@link org.springframework.messaging.Message}. When this
	 * is provided, no {@code payload-expression}s or {@code header}s are allowed; the custom mapper is
	 * responsible for creating the message.
	 * @param mapper the {@link MethodArgsMessageMapper} to use.
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setMapper(MethodArgsMessageMapper)
	 */
	public GatewayProxySpec mapper(MethodArgsMessageMapper mapper) {
		this.gatewayProxyFactoryBean.setMapper(mapper);
		return this;
	}

	/**
	 * Indicate if {@code default} methods on the interface should be proxied as well.
	 * @param proxyDefaultMethods the boolean flag to proxy default methods or invoke via {@code MethodHandle}.
	 * @return current {@link GatewayProxySpec}.
	 * @see GatewayProxyFactoryBean#setProxyDefaultMethods(boolean)
	 * @since 5.3
	 */
	public GatewayProxySpec proxyDefaultMethods(boolean proxyDefaultMethods) {
		this.gatewayProxyFactoryBean.setProxyDefaultMethods(proxyDefaultMethods);
		return this;
	}

	MessageChannel getGatewayRequestChannel() {
		return this.gatewayRequestChannel;
	}

	GatewayProxyFactoryBean getGatewayProxyFactoryBean() {
		if (this.populateGatewayMethodMetadata) {
			this.gatewayMethodMetadata.setHeaderExpressions(this.headerExpressions);
			this.gatewayProxyFactoryBean.setGlobalMethodMetadata(this.gatewayMethodMetadata);
		}
		return this.gatewayProxyFactoryBean;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return Collections.singletonMap(this.gatewayProxyFactoryBean, this.gatewayProxyFactoryBean.getBeanName()
				+ ".gateway");
	}

}
