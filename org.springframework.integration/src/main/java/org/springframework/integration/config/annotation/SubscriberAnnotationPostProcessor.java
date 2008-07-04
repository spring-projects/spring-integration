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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.Subscriber;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.handler.DefaultMessageHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link BeanPostProcessor} that creates a method-invoking handler adapter
 * when it discovers methods annotated with {@link Subscriber @Subscriber}.
 * 
 * @author Mark Fisher
 */
public class SubscriberAnnotationPostProcessor implements BeanPostProcessor {

	private Log logger = LogFactory.getLog(this.getClass());

	private Class<? extends Annotation> subscriberAnnotationType = Subscriber.class;

	private String channelNameAttribute = "channel";

	private MessageBus messageBus;


	public void setSubscriberAnnotationType(Class<? extends Annotation> subscriberAnnotationType) {
		Assert.notNull(subscriberAnnotationType, "'subscriberAnnotationType' must not be null");
		this.subscriberAnnotationType = subscriberAnnotationType;
	}

	public void setChannelNameAttribute(String channelNameAttribute) {
		Assert.notNull(channelNameAttribute, "'channelNameAttribute' must not be null");
		this.channelNameAttribute = channelNameAttribute;
	}

	public void setMessageBus(MessageBus messageBus) {
		Assert.notNull(messageBus, "'messageBus' must not be null");
		this.messageBus = messageBus;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
		final Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (targetClass == null) {
			return bean;
		}
		if (this.messageBus == null) {
			if (logger.isWarnEnabled()) {
				logger.warn(this.getClass().getSimpleName() + " is disabled since no 'messageBus' was provided");
			}
			return bean;
		}
		ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = method.getAnnotation(subscriberAnnotationType);
				if (annotation != null) {
					String channelName = (String) AnnotationUtils.getValue(annotation, channelNameAttribute);
					DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
					adapter.setMethodName(method.getName());
					adapter.setObject(bean);
					adapter.afterPropertiesSet();
					String adapterName = ClassUtils.getShortNameAsProperty(targetClass) + 
							"-" + method.getName() + "-endpoint";
					messageBus.registerHandler(adapterName, adapter, channelName, null);
				}
			}
		});
		return bean;
	}

}
