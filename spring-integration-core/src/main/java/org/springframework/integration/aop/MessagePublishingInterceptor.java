/*
 * Copyright 2002-2022 the original author or authors.
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
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * A {@link MethodInterceptor} that publishes Messages to a channel. The
 * payload of the published Message can be derived from arguments or any return
 * value or exception resulting from the method invocation. That mapping is the
 * responsibility of the EL expression provided by the {@link PublisherMetadataSource}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public class MessagePublishingInterceptor implements MethodInterceptor, BeanFactoryAware {

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private final PublisherMetadataSource metadataSource;

	private DestinationResolver<MessageChannel> channelResolver;

	private BeanFactory beanFactory;

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private String defaultChannelName;

	private volatile boolean messageBuilderFactorySet;

	private volatile boolean templateInitialized;

	public MessagePublishingInterceptor(PublisherMetadataSource metadataSource) {
		Assert.notNull(metadataSource, "metadataSource must not be null");
		this.metadataSource = metadataSource;
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
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override
	public final Object invoke(MethodInvocation invocation) throws Throwable {
		initMessagingTemplateIfAny();
		StandardEvaluationContext context = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		Class<?> targetClass = AopUtils.getTargetClass(invocation.getThis());
		Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
		String[] argumentNames = resolveArgumentNames(method);
		context.setVariable(PublisherMetadataSource.METHOD_NAME_VARIABLE_NAME, method.getName());
		if (invocation.getArguments().length > 0 && argumentNames != null) {
			Map<Object, Object> argumentMap = new HashMap<>();
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
		catch (Throwable t) { //NOSONAR - rethrown below
			context.setVariable(PublisherMetadataSource.EXCEPTION_VARIABLE_NAME, t);
			throw t;
		}
		finally {
			publishMessage(method, context);
		}
	}

	private void initMessagingTemplateIfAny() {
		if (!this.templateInitialized) {
			this.messagingTemplate.setBeanFactory(this.beanFactory);
			if (this.channelResolver == null) {
				this.channelResolver = ChannelResolverUtils.getChannelResolver(this.beanFactory);
			}
			this.templateInitialized = true;
		}
	}

	private String[] resolveArgumentNames(Method method) {
		return this.parameterNameDiscoverer.getParameterNames(method);
	}

	private void publishMessage(Method method, StandardEvaluationContext context) {
		Expression payloadExpression = this.metadataSource.getExpressionForPayload(method);
		if (payloadExpression == null) {
			payloadExpression = PublisherMetadataSource.RETURN_VALUE_EXPRESSION;
		}
		Object result = payloadExpression.getValue(context);
		if (result != null) {
			AbstractIntegrationMessageBuilder<?> builder = (result instanceof Message<?>)
					? getMessageBuilderFactory().fromMessage((Message<?>) result)
					: getMessageBuilderFactory().withPayload(result);
			Map<String, Object> headers = evaluateHeaders(method, context);
			if (headers != null) {
				builder.copyHeaders(headers);
			}
			Message<?> message = builder.build();
			String channelName = this.metadataSource.getChannelName(method);
			if (channelName != null) {
				this.messagingTemplate.send(channelName, message);
			}
			else {
				String channelNameToUse = this.defaultChannelName;
				if (channelNameToUse != null && this.messagingTemplate.getDefaultDestination() == null) {
					Assert.state(this.channelResolver != null, "ChannelResolver is required to resolve channel names.");
					this.messagingTemplate.setDefaultChannel(this.channelResolver.resolveDestination(channelNameToUse));
					this.defaultChannelName = null;
				}
				this.messagingTemplate.send(message);
			}
		}
	}

	private Map<String, Object> evaluateHeaders(Method method, StandardEvaluationContext context) {
		Map<String, Expression> headerExpressionMap = this.metadataSource.getExpressionsForHeaders(method);
		if (headerExpressionMap != null) {
			return ExpressionEvalMap.from(headerExpressionMap)
					.usingEvaluationContext(context)
					.build();
		}

		return null;
	}

}
