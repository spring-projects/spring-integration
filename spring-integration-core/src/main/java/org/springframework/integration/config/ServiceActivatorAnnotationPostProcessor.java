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

package org.springframework.integration.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.ResolvableType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link ServiceActivator @ServiceActivator}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Yilin Wei
 */
public class ServiceActivatorAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<ServiceActivator> {

	public ServiceActivatorAnnotationPostProcessor() {
		this.messageHandlerAttributes.addAll(Arrays.asList("outputChannel", "requiresReply"));
	}

	@Override
	protected BeanDefinition resolveHandlerBeanDefinition(String beanName, AnnotatedBeanDefinition beanDefinition,
			ResolvableType handlerBeanType, List<Annotation> annotations) {

		BeanDefinition handlerBeanDefinition =
				super.resolveHandlerBeanDefinition(beanName, beanDefinition, handlerBeanType, annotations);

		if (handlerBeanDefinition != null) {
			return handlerBeanDefinition;
		}

		BeanMetadataElement targetObjectBeanDefinition = buildLambdaMessageProcessor(handlerBeanType, beanDefinition);
		if (targetObjectBeanDefinition == null) {
			targetObjectBeanDefinition = new RuntimeBeanReference(beanName);
		}

		BeanDefinition serviceActivatorBeanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition(ServiceActivatorFactoryBean.class)
						.addPropertyValue("targetObject", targetObjectBeanDefinition)
						.getBeanDefinition();

		new BeanDefinitionPropertiesMapper(serviceActivatorBeanDefinition, annotations)
				.setPropertyValue("requiresReply")
				.setPropertyValue("async");

		return serviceActivatorBeanDefinition;
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		AbstractReplyProducingMessageHandler serviceActivator = new ServiceActivatingHandler(bean, method);

		String requiresReply = MessagingAnnotationUtils.resolveAttribute(annotations, "requiresReply", String.class);
		if (StringUtils.hasText(requiresReply)) {
			serviceActivator.setRequiresReply(resolveAttributeToBoolean(requiresReply));
		}

		String isAsync = MessagingAnnotationUtils.resolveAttribute(annotations, "async", String.class);
		if (StringUtils.hasText(isAsync)) {
			serviceActivator.setAsync(resolveAttributeToBoolean(isAsync));
		}

		setOutputChannelIfPresent(annotations, serviceActivator);
		return serviceActivator;
	}

}
