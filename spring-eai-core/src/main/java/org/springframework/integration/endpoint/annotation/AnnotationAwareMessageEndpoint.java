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

package org.springframework.integration.endpoint.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.endpoint.InboundMethodInvokingChannelAdapter;
import org.springframework.integration.endpoint.MessageHandlerAdapter;
import org.springframework.integration.endpoint.OutboundMethodInvokingChannelAdapter;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * An endpoint implementation for classes annotated with {@link MessageEndpoint @MessageEndpoint}.
 * 
 * @author Mark Fisher
 */
public class AnnotationAwareMessageEndpoint extends GenericMessageEndpoint implements InitializingBean {

	private Object targetObject;

	private MessageBus messageBus;


	public AnnotationAwareMessageEndpoint(Object targetObject, MessageBus messageBus) {
		Assert.notNull(targetObject, "targetObject must not be null");
		Assert.notNull(messageBus, "messageBus must not be null");
		this.targetObject = targetObject;
		this.messageBus = messageBus;
	}


	public void afterPropertiesSet() {
		this.configureInputChannel();
		this.configureDefaultOutputChannel();
		this.createHandlerChain();
		this.messageBus.registerEndpoint(ClassUtils.getShortNameAsProperty(targetObject.getClass()), this);
	}

	private void configureInputChannel() {
		MessageEndpoint endpointAnnotation = this.targetObject.getClass().getAnnotation(MessageEndpoint.class);
		String channelName = endpointAnnotation.input();
		if (StringUtils.hasText(channelName)) {
			this.setInputChannelName(channelName);
			ConsumerPolicy consumerPolicy = new ConsumerPolicy();
			consumerPolicy.setPeriod(endpointAnnotation.pollPeriod());
			this.setConsumerPolicy(consumerPolicy);
		}
		else {
			ReflectionUtils.doWithMethods(targetObject.getClass(), new ReflectionUtils.MethodCallback() {
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					Annotation annotation = AnnotationUtils.getAnnotation(method, Polled.class);
					if (annotation != null) {
						InboundMethodInvokingChannelAdapter<Object> adapter =
								new InboundMethodInvokingChannelAdapter<Object>();
						adapter.setObject(targetObject);
						adapter.setMethod(method.getName());
						adapter.afterPropertiesSet();
						String channelName = ClassUtils.getShortNameAsProperty(
								targetObject.getClass()) + "-inputChannel";
						messageBus.registerChannel(channelName, adapter);
						setInputChannelName(channelName);
						return;
					}
				}
			});
		}
	}

	private void configureDefaultOutputChannel() {
		MessageEndpoint endpointAnnotation = this.targetObject.getClass().getAnnotation(MessageEndpoint.class);
		String channelName = endpointAnnotation.defaultOutput();
		if (StringUtils.hasText(channelName)) {
			this.setDefaultOutputChannelName(channelName);
		}
		else {
			ReflectionUtils.doWithMethods(targetObject.getClass(), new ReflectionUtils.MethodCallback() {
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					Annotation annotation = AnnotationUtils.getAnnotation(method, DefaultOutput.class);
					if (annotation != null) {
						OutboundMethodInvokingChannelAdapter<Object> adapter = 
								new OutboundMethodInvokingChannelAdapter<Object>();
						adapter.setObject(targetObject);
						adapter.setMethod(method.getName());
						adapter.afterPropertiesSet();
						String channelName = ClassUtils.getShortNameAsProperty(
								targetObject.getClass()) + "-defaultOutputChannel";
						messageBus.registerChannel(channelName, adapter);
						setDefaultOutputChannelName(channelName);
						return;
					}
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	private void createHandlerChain() {
		final List<MessageHandlerAdapter<?>> handlers = new ArrayList<MessageHandlerAdapter<?>>();
		ReflectionUtils.doWithMethods(targetObject.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.getAnnotation(method, Handler.class);
				if (annotation != null) {
					MessageHandlerAdapter<Object> adapter = new MessageHandlerAdapter<Object>();
					adapter.setObject(targetObject);
					adapter.setMethod(method.getName());
					adapter.afterPropertiesSet();
					Order orderAnnotation = (Order) AnnotationUtils.getAnnotation(method, Order.class);
					if (orderAnnotation != null) {
						adapter.setOrder(orderAnnotation.value());
					}
					handlers.add(adapter);
				}
			}
		});
		if (handlers.size() > 0) {
			MessageHandlerChain handlerChain = new MessageHandlerChain();
			Collections.sort(handlers, new OrderComparator());
			for (MessageHandler handler : handlers) {
				handlerChain.add(handler);
			}
			this.setHandler(handlerChain);
		}
	}

}
