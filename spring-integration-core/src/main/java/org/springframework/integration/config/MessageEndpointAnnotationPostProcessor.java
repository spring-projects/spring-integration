/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.endpoint.InboundMethodInvokingChannelAdapter;
import org.springframework.integration.endpoint.OutboundMethodInvokingChannelAdapter;
import org.springframework.integration.endpoint.annotation.DefaultOutput;
import org.springframework.integration.endpoint.annotation.MessageEndpoint;
import org.springframework.integration.endpoint.annotation.Polled;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.handler.annotation.AnnotationHandlerCreator;
import org.springframework.integration.handler.annotation.DefaultAnnotationHandlerCreator;
import org.springframework.integration.handler.annotation.Handler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanPostProcessor} implementation that generates endpoints for
 * classes annotated with {@link MessageEndpoint @MessageEndpoint}.
 * 
 * @author Mark Fisher
 */
public class MessageEndpointAnnotationPostProcessor implements BeanPostProcessor, InitializingBean {

	private Map<Class<? extends Annotation>, AnnotationHandlerCreator> handlerCreators =
			new ConcurrentHashMap<Class<? extends Annotation>, AnnotationHandlerCreator>();

	private MessageBus messageBus;


	public void setMessageBus(MessageBus messageBus) {
		Assert.notNull(messageBus, "messageBus must not be null");
		this.messageBus = messageBus;
	}

	public void setCustomHandlerCreators(
			Map<Class<? extends Annotation>, AnnotationHandlerCreator> customHandlerCreators) {
		for (Map.Entry<Class<? extends Annotation>, AnnotationHandlerCreator> entry : customHandlerCreators.entrySet()) {
			this.handlerCreators.put(entry.getKey(), entry.getValue());
		}
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.messageBus, "messageBus is required");
		this.handlerCreators.put(Handler.class, new DefaultAnnotationHandlerCreator());
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		MessageEndpoint endpointAnnotation = bean.getClass().getAnnotation(MessageEndpoint.class);
		if (endpointAnnotation == null) {
			return bean;
		}
		GenericMessageEndpoint endpoint = new GenericMessageEndpoint();
		this.configureInputChannel(bean, beanName, endpointAnnotation, endpoint);
		this.configureDefaultOutputChannel(bean, beanName, endpointAnnotation, endpoint);
		MessageHandlerChain handlerChain = this.createHandlerChain(bean);
		if (handlerChain != null) {
			endpoint.setHandler(handlerChain);
		}
		this.messageBus.registerEndpoint(beanName, endpoint);
		return endpoint;
	}

	private void configureInputChannel(final Object bean, final String beanName,
			MessageEndpoint annotation, final GenericMessageEndpoint endpoint) {
		String channelName = annotation.input();
		if (StringUtils.hasText(channelName)) {
			endpoint.setInputChannelName(channelName);
			ConsumerPolicy consumerPolicy = new ConsumerPolicy();
			consumerPolicy.setPeriod(annotation.pollPeriod());
			endpoint.setConsumerPolicy(consumerPolicy);
			return;
		}
		ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.getAnnotation(method, Polled.class);
				if (annotation != null) {
					InboundMethodInvokingChannelAdapter<Object> adapter = new InboundMethodInvokingChannelAdapter<Object>();
					adapter.setObject(bean);
					adapter.setMethod(method.getName());
					adapter.afterPropertiesSet();
					String channelName = beanName + "-inputChannel";
					messageBus.registerChannel(channelName, adapter);
					endpoint.setInputChannelName(channelName);
					int period = ((Polled) annotation).period();
					endpoint.getConsumerPolicy().setPeriod(period);
					if (period > 0) {
						endpoint.getConsumerPolicy().setConcurrency(1);
						endpoint.getConsumerPolicy().setMaxConcurrency(1);
						endpoint.getConsumerPolicy().setMaxMessagesPerTask(1);
					}
					return;
				}
			}
		});
	}

	private void configureDefaultOutputChannel(final Object bean, final String beanName,
			final MessageEndpoint annotation, final GenericMessageEndpoint endpoint) {
		String channelName = annotation.defaultOutput();
		if (StringUtils.hasText(channelName)) {
			endpoint.setDefaultOutputChannelName(channelName);
			return;
		}
		ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.getAnnotation(method, DefaultOutput.class);
				if (annotation != null) {
					OutboundMethodInvokingChannelAdapter<Object> adapter = new OutboundMethodInvokingChannelAdapter<Object>();
					adapter.setObject(bean);
					adapter.setMethod(method.getName());
					adapter.afterPropertiesSet();
					String channelName = beanName + "-defaultOutputChannel";
					messageBus.registerChannel(channelName, adapter);
					endpoint.setDefaultOutputChannelName(channelName);
					return;
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	private MessageHandlerChain createHandlerChain(final Object bean) {
		final List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				for (Class<? extends Annotation> annotationType : handlerCreators.keySet()) {
					Annotation annotation = AnnotationUtils.getAnnotation(method, annotationType);
					if (annotation != null) {
						MessageHandler handler = handlerCreators.get(annotationType).createHandler(bean, method, annotation);
						if (handler != null) {
							handlers.add(handler);
						}
					}
				}
			}
		});
		if (handlers.size() > 0) {
			MessageHandlerChain handlerChain = new MessageHandlerChain();
			Collections.sort(handlers, new OrderComparator());
			for (MessageHandler handler : handlers) {
				handlerChain.add(handler);
			}
			return handlerChain;
		}
		return null;
	}

}
