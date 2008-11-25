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
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolutionException;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.MethodInvokingMessageHandler;
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
		MessageChannel channel = this.resolveOrCreateChannel(annotation.value());
		Assert.isInstanceOf(SubscribableChannel.class, channel,
				"The channel for an Annotation-based Channel Adapter must be a SubscribableChannel.");
		Assert.isTrue(method.getParameterTypes().length > 0,
				"A method annotated with @ChannelAdapter must accept at least one argument.");
		Assert.isTrue(!hasReturnValue(method),
				"A method annotated with @ChannelAdapter must not have a return value."
				+ " Consider using a @ServiceActivator if a reply Message is expected.");
		MethodInvokingMessageHandler handler = new MethodInvokingMessageHandler(bean, method);
		EventDrivenConsumer endpoint = new EventDrivenConsumer((SubscribableChannel) channel, handler);
		String annotationName = ClassUtils.getShortNameAsProperty(annotation.annotationType());
		String endpointName = beanName + "." + method.getName() + "." + annotationName;
		this.beanFactory.registerSingleton(endpointName, endpoint);
		endpoint.setBeanFactory(beanFactory);
		endpoint.afterPropertiesSet();
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

	private boolean hasReturnValue(Method method) {
		return !method.getReturnType().equals(void.class);
	}

}
