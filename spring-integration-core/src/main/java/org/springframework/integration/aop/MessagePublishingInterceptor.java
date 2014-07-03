/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link MethodInterceptor} that publishes Messages to a channel. The
 * payload of the published Message can be derived from arguments or any return
 * value or exception resulting from the method invocation. That mapping is the
 * responsibility of the EL expression provided by the {@link PublisherMetadataSource}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
public class MessagePublishingInterceptor implements MethodInterceptor, BeanFactoryAware {

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile PublisherMetadataSource metadataSource;

	private final ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private volatile DestinationResolver<MessageChannel> channelResolver;

	private volatile BeanFactory beanFactory;

	private final ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile String defaultChannelName;

	public MessagePublishingInterceptor(PublisherMetadataSource metadataSource) {
		Assert.notNull(metadataSource, "metadataSource must not be null");
		this.metadataSource = metadataSource;
	}


	public void setPublisherMetadataSource(PublisherMetadataSource metadataSource) {
		Assert.notNull(metadataSource, "metadataSource must not be null");
		this.metadataSource = metadataSource;
	}

	/**
	 * @deprecated Use {@link #setDefaultChannelName(String)}.
	 * @param defaultChannel the default channel.
	 */
	@Deprecated
	public void setDefaultChannel(MessageChannel defaultChannel) {
		this.messagingTemplate.setDefaultDestination(defaultChannel);
		this.defaultChannelName = null;
	}

	/**
	 * @param defaultChannelName the default channel name.
	 * @since 4.0.3
	 */
	public void setDefaultChannelName(String defaultChannelName) {
		this.defaultChannelName = defaultChannelName;
	}

	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		this.channelResolver = channelResolver;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		this.messagingTemplate.setBeanFactory(beanFactory);
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(beanFactory);
	}

	@Override
	public final Object invoke(final MethodInvocation invocation) throws Throwable {
		Assert.notNull(this.metadataSource, "PublisherMetadataSource is required.");
		final StandardEvaluationContext context = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		Class<?> targetClass = AopUtils.getTargetClass(invocation.getThis());
		final Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
		String[] argumentNames = this.resolveArgumentNames(method);
		context.setVariable(PublisherMetadataSource.METHOD_NAME_VARIABLE_NAME, method.getName());
		if (invocation.getArguments().length > 0 && argumentNames != null) {
			Map<Object, Object> argumentMap = new HashMap<Object, Object>();
			for (int i = 0; i < argumentNames.length; i++) {
				if (invocation.getArguments().length <= i) {
					break;
				}
				Object argValue = invocation.getArguments()[i];
				argumentMap.put(i, argValue);
				argumentMap.put(argumentNames[i], argValue);
			}
			context.setVariable(PublisherMetadataSource.ARGUMENT_MAP_VARIABLE_NAME, argumentMap);
		}
		try {
			Object returnValue = invocation.proceed();
			context.setVariable(PublisherMetadataSource.RETURN_VALUE_VARIABLE_NAME, returnValue);
			return returnValue;
		}
		catch (Throwable t) {
			context.setVariable(PublisherMetadataSource.EXCEPTION_VARIABLE_NAME, t);
			throw t;
		}
		finally {
			publishMessage(method, context);
		}
	}

	private String[] resolveArgumentNames(Method method) {
		return this.parameterNameDiscoverer.getParameterNames(method);
	}

	private void publishMessage(Method method, StandardEvaluationContext context) throws Exception {
		String payloadExpressionString = this.metadataSource.getPayloadExpression(method);
		if (!StringUtils.hasText(payloadExpressionString)) {
			payloadExpressionString = "#" + PublisherMetadataSource.RETURN_VALUE_VARIABLE_NAME;
		}
		Expression expression = this.parser.parseExpression(payloadExpressionString);
		Object result = expression.getValue(context);
		if (result != null) {
			AbstractIntegrationMessageBuilder<?> builder = (result instanceof Message<?>)
					? this.messageBuilderFactory.fromMessage((Message<?>) result)
					: this.messageBuilderFactory.withPayload(result);
			Map<String, Object> headers = this.evaluateHeaders(method, context);
			if (headers != null) {
				builder.copyHeaders(headers);
			}
			Message<?> message = builder.build();
			String channelName = this.metadataSource.getChannelName(method);
			MessageChannel channel = null;
			if (channelName != null) {
				Assert.state(this.channelResolver != null, "ChannelResolver is required to resolve channel names.");
				channel = this.channelResolver.resolveDestination(channelName);
			}
			if (channel != null) {
				this.messagingTemplate.send(channel, message);
			}
			else {
				if (this.defaultChannelName != null) {
					synchronized(this) {
						if (this.defaultChannelName != null && this.messagingTemplate.getDefaultDestination() == null) {
							Assert.state(this.channelResolver != null,
									"ChannelResolver is required to resolve channel names.");
							this.messagingTemplate.setDefaultChannel(
									this.channelResolver.resolveDestination(this.defaultChannelName));
						}
						this.defaultChannelName = null;
					}
				}
				this.messagingTemplate.send(message);
			}
		}
	}

	private Map<String, Object> evaluateHeaders(Method method, StandardEvaluationContext context)
			throws ParseException, EvaluationException {

		Map<String, String> headerExpressionMap = this.metadataSource.getHeaderExpressions(method);
		if (headerExpressionMap != null) {
			Map<String, Object> headers = new HashMap<String, Object>();
			for (Map.Entry<String, String> headerExpressionEntry : headerExpressionMap.entrySet()) {
				String headerExpression = headerExpressionEntry.getValue();
				if (StringUtils.hasText(headerExpression)) {
					Expression expression = this.parser.parseExpression(headerExpression);
					Object result = expression.getValue(context);
					if (result != null) {
						headers.put(headerExpressionEntry.getKey(), result);
					}
				}
			}
			if (headers.size() > 0) {
				return headers;
			}
		}
		return null;
	}

}
