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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.generic.GenericBeanFactoryAccessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for Method-level annotation post-processors.
 *
 * @author Mark Fisher
 */
public abstract class AbstractMethodAnnotationPostProcessor<T extends Annotation> implements MethodAnnotationPostProcessor<T> {

	private static final String INPUT_CHANNEL_ATTRIBUTE = "inputChannel";


	protected final GenericBeanFactoryAccessor beanFactoryAccessor;

	protected final ChannelResolver channelResolver;


	public AbstractMethodAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactoryAccessor = new GenericBeanFactoryAccessor(beanFactory);
		this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
	}


	public Object postProcess(Object bean, String beanName, Method method, T annotation) {
		MessageHandler handler = this.createHandler(bean, method, annotation);
		Poller pollerAnnotation = AnnotationUtils.findAnnotation(method, Poller.class);
		MessageEndpoint endpoint = this.createEndpoint(handler, annotation, pollerAnnotation);
		if (endpoint != null) {
			if (endpoint instanceof InitializingBean) {
				try {
					((InitializingBean) endpoint).afterPropertiesSet();
				}
				catch (Exception e) {
					throw new IllegalStateException(
							"failed to initialize annotation-based consumer", e);
				}
			}
			return endpoint;
		}
		return handler;
	}

	protected boolean shouldCreateEndpoint(T annotation) {
		return (StringUtils.hasText((String) AnnotationUtils.getValue(annotation, INPUT_CHANNEL_ATTRIBUTE)));
	}

	private MessageEndpoint createEndpoint(MessageHandler handler, T annotation, Poller pollerAnnotation) {
		MessageEndpoint endpoint = null;
		String inputChannelName = (String) AnnotationUtils.getValue(annotation, INPUT_CHANNEL_ATTRIBUTE);
		if (StringUtils.hasText(inputChannelName)) {
			MessageChannel inputChannel = this.channelResolver.resolveChannelName(inputChannelName);
			Assert.notNull(inputChannel, "failed to resolve inputChannel '" + inputChannelName + "'");
			if (inputChannel instanceof PollableChannel) {
				PollingConsumer pollingEndpoint = new PollingConsumer(
						(PollableChannel) inputChannel, handler);
				if (pollerAnnotation != null) {
					AnnotationConfigUtils.configurePollingEndpointWithPollerAnnotation(
							pollingEndpoint, pollerAnnotation, this.beanFactoryAccessor.getBeanFactory());
				}
				endpoint = pollingEndpoint;
			}
			else if (inputChannel instanceof SubscribableChannel) {
				Assert.isTrue(pollerAnnotation == null,
						"The @Poller annotation should only be provided for a PollableChannel");
				endpoint = new SubscribingConsumerEndpoint((SubscribableChannel) inputChannel, handler);
			}
			else {
				throw new IllegalArgumentException("unsupported channel type: [" + inputChannel.getClass() + "]");
			}
			if (handler instanceof BeanFactoryAware) {
				((BeanFactoryAware) handler).setBeanFactory(this.beanFactoryAccessor.getBeanFactory());
			}
		}
		return endpoint;
	}

	/**
	 * Subclasses must implement this method to create the MessageHandler.
	 */
	protected abstract MessageHandler createHandler(Object bean, Method method, T annotation);

}
