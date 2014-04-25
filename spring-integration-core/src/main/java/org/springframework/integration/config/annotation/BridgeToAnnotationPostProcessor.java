/*
 * Copyright 2014 the original author or authors.
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
import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Post-processor for the {@link BridgeTo @BridgeTo} annotation.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class BridgeToAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<BridgeTo> {

	public BridgeToAnnotationPostProcessor(ListableBeanFactory beanFactory, Environment environment) {
		super(beanFactory, environment);
	}

	@Override
	public boolean shouldCreateEndpoint(Method method, List<Annotation> annotations) {
		boolean isBean = AnnotatedElementUtils.isAnnotated(method, Bean.class.getName());
		Assert.isTrue(isBean, "'@BridgeTo' is eligible only for '@Bean' methods");

		boolean isMessageChannelBean = MessageChannel.class.isAssignableFrom(method.getReturnType());
		Assert.isTrue(isMessageChannelBean, "'@BridgeTo' is eligible only for 'MessageChannel' '@Bean' methods");

		boolean hasBridgeFrom = AnnotatedElementUtils.isAnnotated(method, BridgeFrom.class.getName());

		Assert.isTrue(!hasBridgeFrom, "'@BridgeFrom' and '@BridgeTo' are mutually exclusive 'MessageChannel' " +
				"'@Bean' method annotations");

		return true;
	}

	@Override
	public Object postProcess(Object bean, String beanName, Method method, List<Annotation> annotations) {
		MessageHandler handler = createHandler(bean, method, annotations);
		setAdviceChainIfPresent(beanName, annotations, handler);
		if (handler instanceof Orderable) {
			Order orderAnnotation = AnnotationUtils.findAnnotation(method, Order.class);
			if (orderAnnotation != null) {
				((Orderable) handler).setOrder(orderAnnotation.value());
			}
		}
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			String handlerBeanName = generateHandlerBeanName(beanName, method);
			ConfigurableListableBeanFactory listableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
			listableBeanFactory.registerSingleton(handlerBeanName, handler);
			handler = (MessageHandler) listableBeanFactory.initializeBean(handler, handlerBeanName);
		}
		AbstractEndpoint endpoint = createEndpoint(handler, method, annotations);
		if (endpoint != null) {
			return endpoint;
		}
		return handler;
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		BridgeHandler handler = new BridgeHandler();
		String outputChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, "value", String.class);
		handler.setOutputChannelName(outputChannelName);
		return handler;
	}

	private AbstractEndpoint createEndpoint(MessageHandler handler, Method method, List<Annotation> annotations) {
		String inputChannelName = null;
		String[] names = AnnotationUtils.getAnnotation(method, Bean.class).name();
		if (!ObjectUtils.isEmpty(names)) {
			inputChannelName = names[0];
		}
		if (!StringUtils.hasText(inputChannelName)) {
			inputChannelName = method.getName();
		}

		MessageChannel inputChannel = this.beanFactory.getBean(inputChannelName, MessageChannel.class);

		if (inputChannel instanceof PollableChannel) {
			PollingConsumer pollingConsumer = new PollingConsumer((PollableChannel) inputChannel, handler);
			configurePollingEndpoint(pollingConsumer, annotations);
			return pollingConsumer;
		}
		else {
			Poller[] pollers = MessagingAnnotationUtils.resolveAttribute(annotations, "poller", Poller[].class);
			Assert.state(ObjectUtils.isEmpty(pollers), "A '@Poller' should not be specified for Annotation-based " +
					"endpoint, since '" + inputChannel + "' is a SubscribableChannel (not pollable).");
			return new EventDrivenConsumer((SubscribableChannel) inputChannel, handler);
		}
	}

}
