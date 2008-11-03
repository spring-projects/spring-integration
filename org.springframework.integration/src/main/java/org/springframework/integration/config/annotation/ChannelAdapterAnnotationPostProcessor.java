/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.lang.reflect.Method;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolutionException;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.MethodInvokingMessageHandler;
import org.springframework.integration.message.MethodInvokingMessageSource;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Post-processor for methods annotated with {@link ChannelAdapter @ChannelAdapter}.
 *
 * @author Mark Fisher
 */
public class ChannelAdapterAnnotationPostProcessor implements MethodAnnotationPostProcessor<ChannelAdapter> {

	private final ConfigurableBeanFactory beanFactory;

	private final ChannelResolver channelResolver;


	public ChannelAdapterAnnotationPostProcessor(ConfigurableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.channelResolver = new BeanFactoryChannelResolver(this.beanFactory);
	}


	public Object postProcess(Object bean, String beanName, Method method, ChannelAdapter annotation) {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");
		MessageEndpoint endpoint = null;
		MessageChannel channel = this.resolveOrCreateChannel(annotation.value());
		Poller pollerAnnotation = AnnotationUtils.findAnnotation(method, Poller.class);
		if (method.getParameterTypes().length == 0 && hasReturnValue(method)) {
			MethodInvokingMessageSource source = new MethodInvokingMessageSource();
			source.setObject(bean);
			source.setMethod(method);
			endpoint = this.createInboundChannelAdapter(source, channel, pollerAnnotation);
		}
		else if (method.getParameterTypes().length > 0 && !hasReturnValue(method)) {
			MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(bean, method);
			endpoint = this.createOutboundChannelAdapter(channel, handler, pollerAnnotation);
		}
		else {
			throw new IllegalArgumentException("The @ChannelAdapter can only be applied to methods"
					+ " that accept no arguments but have a return value (inbound) or methods that"
					+ " have no return value but do accept arguments (outbound).");
		}
		if (endpoint != null) {
			String annotationName = ClassUtils.getShortNameAsProperty(annotation.annotationType());
			String endpointName = beanName + "." + method.getName() + "." + annotationName;
			this.beanFactory.registerSingleton(endpointName, endpoint);
		}
		return bean;
	}

	private MessageChannel resolveOrCreateChannel(String channelName) {
		try {
			return this.channelResolver.resolveChannelName(channelName);
		}
		catch (ChannelResolutionException e) {
			DirectChannel directChannel = new DirectChannel();
			directChannel.setBeanName(channelName);
			this.beanFactory.registerSingleton(channelName, directChannel);
			return directChannel;
		}
	}

	private SourcePollingChannelAdapter createInboundChannelAdapter(MethodInvokingMessageSource source, MessageChannel channel, Poller pollerAnnotation) {
		Assert.notNull(pollerAnnotation, "The @Poller annotation is required (at method-level) "
					+ "when using the @ChannelAdapter annotation with a no-arg method.");
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		adapter.setSource(source);
		adapter.setOutputChannel(channel);
		AnnotationConfigUtils.configurePollingEndpointWithPollerAnnotation(
				adapter, pollerAnnotation, this.beanFactory);
		return adapter;
	}

	private MessageEndpoint createOutboundChannelAdapter(MessageChannel channel, MethodInvokingMessageHandler handler, Poller pollerAnnotation) {
		if (channel instanceof PollableChannel) {
			Trigger trigger = (pollerAnnotation != null)
					? AnnotationConfigUtils.parseTriggerFromPollerAnnotation(pollerAnnotation)
					: new IntervalTrigger(0);
			PollingConsumer endpoint = new PollingConsumer((PollableChannel) channel, handler);
			endpoint.setTrigger(trigger);
			return endpoint;
		}
		if (channel instanceof SubscribableChannel) {
			return new EventDrivenConsumer((SubscribableChannel) channel, handler);
		}
		return null;
	}

	private boolean hasReturnValue(Method method) {
		return !method.getReturnType().equals(void.class);
	}

}
