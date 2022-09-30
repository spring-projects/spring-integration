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
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link Filter @Filter}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class FilterAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Filter> {

	public FilterAnnotationPostProcessor() {
		this.messageHandlerAttributes.addAll(Arrays.asList("discardChannel", "throwExceptionOnRejection",
				"adviceChain", "discardWithinAdvice"));
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

		BeanDefinition filterBeanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition(FilterFactoryBean.class)
						.addPropertyValue("targetObject", targetObjectBeanDefinition)
						.getBeanDefinition();

		new BeanDefinitionPropertiesMapper(filterBeanDefinition, annotations)
				.setPropertyValue("discardWithinAdvice")
				.setPropertyValue("throwExceptionOnRejection")
				.setPropertyReference("discardChannel");

		return filterBeanDefinition;
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		Assert.isTrue(boolean.class.equals(method.getReturnType()) || Boolean.class.equals(method.getReturnType()),
				"The Filter annotation may only be applied to methods with a boolean return type.");
		MessageSelector selector = new MethodInvokingSelector(bean, method);

		MessageFilter filter = new MessageFilter(selector);

		String discardWithinAdvice =
				MessagingAnnotationUtils.resolveAttribute(annotations, "discardWithinAdvice", String.class);
		if (StringUtils.hasText(discardWithinAdvice)) {
			filter.setDiscardWithinAdvice(resolveAttributeToBoolean(discardWithinAdvice));
		}

		String throwExceptionOnRejection =
				MessagingAnnotationUtils.resolveAttribute(annotations, "throwExceptionOnRejection", String.class);
		if (StringUtils.hasText(throwExceptionOnRejection)) {
			filter.setThrowExceptionOnRejection(resolveAttributeToBoolean(throwExceptionOnRejection));
		}

		String discardChannelName =
				MessagingAnnotationUtils.resolveAttribute(annotations, "discardChannel", String.class);
		if (StringUtils.hasText(discardChannelName)) {
			filter.setDiscardChannelName(discardChannelName);
		}

		setOutputChannelIfPresent(annotations, filter);
		return filter;
	}

}
