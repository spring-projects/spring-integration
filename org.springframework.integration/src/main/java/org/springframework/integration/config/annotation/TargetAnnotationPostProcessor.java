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
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Concurrency;
import org.springframework.integration.annotation.Polled;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.TargetEndpoint;
import org.springframework.integration.handler.MethodInvokingTarget;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.scheduling.Subscription;

/**
 * Post-processor for classes annotated with {@link MessageTarget @MessageTarget}.
 *
 * @author Mark Fisher
 */
public class TargetAnnotationPostProcessor extends AbstractAnnotationMethodPostProcessor<MessageTarget> {

	public TargetAnnotationPostProcessor(MessageBus messageBus, ClassLoader beanClassLoader) {
		super(org.springframework.integration.annotation.MessageTarget.class, messageBus, beanClassLoader);
	}


	public MessageTarget processMethod(Object bean, Method method, Annotation annotation) {
		MethodInvokingTarget target = new MethodInvokingTarget();
		target.setObject(bean);
		target.setMethod(method);
		return target;
	}

	protected MessageTarget processResults(List<MessageTarget> results) {
		if (results.size() > 1) {
			throw new ConfigurationException("At most one @MessageTarget annotation is allowed per class.");
		}
		return (results.size() == 1) ? results.get(0) : null;
	}

	public MessageEndpoint createEndpoint(Object bean, String beanName, Class<?> originalBeanClass,
			org.springframework.integration.annotation.MessageEndpoint endpointAnnotation) {
		TargetEndpoint endpoint = new TargetEndpoint((MessageTarget) bean);
		Polled polledAnnotation = AnnotationUtils.findAnnotation(originalBeanClass, Polled.class);
		Subscription subscription = this.createSubscription(bean, beanName, endpointAnnotation, polledAnnotation);
		endpoint.setSubscription(subscription);
		Concurrency concurrencyAnnotation = AnnotationUtils.findAnnotation(originalBeanClass, Concurrency.class);
		if (concurrencyAnnotation != null) {
			ConcurrencyPolicy concurrencyPolicy = new ConcurrencyPolicy(concurrencyAnnotation.coreSize(),
					concurrencyAnnotation.maxSize());
			concurrencyPolicy.setKeepAliveSeconds(concurrencyAnnotation.keepAliveSeconds());
			concurrencyPolicy.setQueueCapacity(concurrencyAnnotation.queueCapacity());
			endpoint.setConcurrencyPolicy(concurrencyPolicy);
		}
		return endpoint;
	}

}
