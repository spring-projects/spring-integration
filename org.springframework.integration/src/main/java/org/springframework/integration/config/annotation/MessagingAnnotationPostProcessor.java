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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * A {@link BeanPostProcessor} implementation that processes method-level
 * messaging annotations such as @Handler, @MessageSource, and @MessageTarget.
 * It also generates endpoints for classes annotated with the class-level
 * {@link MessageEndpoint @MessageEndpoint} annotation.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class MessagingAnnotationPostProcessor implements BeanPostProcessor, InitializingBean, BeanClassLoaderAware {

	private final MessageBus messageBus;

	private volatile ClassLoader beanClassLoader;

	private final Map<Class<?>, AnnotationMethodPostProcessor> postProcessors =
			new HashMap<Class<?>, AnnotationMethodPostProcessor>();


	public MessagingAnnotationPostProcessor(MessageBus messageBus) {
		Assert.notNull(messageBus, "MessageBus must not be null.");
		this.messageBus = messageBus;
	}


	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	public void afterPropertiesSet() {
		this.postProcessors.put(MessageHandler.class, new HandlerAnnotationPostProcessor(this.messageBus, this.beanClassLoader));
		this.postProcessors.put(MessageSource.class, new PollableAnnotationPostProcessor(this.messageBus, this.beanClassLoader));
		this.postProcessors.put(MessageTarget.class, new TargetAnnotationPostProcessor(this.messageBus, this.beanClassLoader));
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Object originalBean = bean;
		Class<?> beanClass = this.getBeanClass(bean);
		if (!this.isStereotype(beanClass)) {
			// we only post-process stereotype components
			return bean;
		}
		MessageEndpoint endpointAnnotation = AnnotationUtils.findAnnotation(beanClass, MessageEndpoint.class);
		for (Map.Entry<Class<?>, AnnotationMethodPostProcessor> entry : this.postProcessors.entrySet()) {
			AnnotationMethodPostProcessor postProcessor = entry.getValue();
			bean = postProcessor.postProcess(bean, beanName, beanClass);
			if (endpointAnnotation != null && entry.getKey().isAssignableFrom(bean.getClass())) {
				org.springframework.integration.endpoint.MessageEndpoint endpoint =
						postProcessor.createEndpoint(bean, beanName, beanClass, endpointAnnotation);
				if (endpoint != null) {
					endpoint.setBeanName(beanName + "." + entry.getKey().getSimpleName() + ".endpoint");
					Poller pollerAnnotation = AnnotationUtils.findAnnotation(beanClass, Poller.class);
					if (pollerAnnotation != null) {
						PollingSchedule schedule = new PollingSchedule(pollerAnnotation.period());
						schedule.setInitialDelay(pollerAnnotation.initialDelay());
						schedule.setFixedRate(pollerAnnotation.fixedRate());
						schedule.setTimeUnit(pollerAnnotation.timeUnit());
						String inputChannelName = endpointAnnotation.input();
						MessageChannel inputChannel = this.messageBus.lookupChannel(inputChannelName);
						if (inputChannel != null) {
							if (inputChannel instanceof PollableChannel) {
								PollingDispatcher poller = new PollingDispatcher((PollableChannel) inputChannel, schedule);
								poller.setMaxMessagesPerPoll(pollerAnnotation.maxMessagesPerPoll());
								endpoint.setSource(poller);
							}
							else {
								endpoint.setSource(inputChannel);
							}
						}
						else {
							endpoint.setInputChannelName(inputChannelName);
						}
						endpoint.setSchedule(schedule);
					}
					this.messageBus.registerEndpoint(endpoint);
				}
			}
		}
		if (bean instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) bean).setChannelRegistry(this.messageBus);
		}
		if (!bean.equals(originalBean) && originalBean instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) originalBean).setChannelRegistry(this.messageBus);
		}
		if (endpointAnnotation != null && bean.equals(originalBean)) {
			throw new ConfigurationException("Class [" + beanClass.getName()
					+ "] is annotated with @MessageEndpoint but contains no source, target, or handler method annotations.");
		}
		return bean;
	}

	private Class<?> getBeanClass(Object bean) {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		return (targetClass != null) ? targetClass : bean.getClass();
	}

	private boolean isStereotype(Class<?> beanClass) {
		List<Annotation> annotations = new ArrayList<Annotation>(Arrays.asList(beanClass.getAnnotations()));
		Class<?>[] interfaces = beanClass.getInterfaces();
		for (Class<?> iface : interfaces) {
			annotations.addAll(Arrays.asList(iface.getAnnotations()));
		}
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> annotationType = annotation.annotationType();
			if (annotationType.equals(Component.class) || annotationType.isAnnotationPresent(Component.class)) {
				return true;
			}
		}
		return false;
	}

}
