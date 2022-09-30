/*
 * Copyright 2014-2022 the original author or authors.
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
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.util.ClassUtils;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * Post-processor for Methods annotated with {@link InboundChannelAdapter @InboundChannelAdapter}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Chris Bono
 *
 * @since 4.0
 */
public class InboundChannelAdapterAnnotationPostProcessor extends
		AbstractMethodAnnotationPostProcessor<InboundChannelAdapter> {

	@Override
	public String getInputChannelAttribute() {
		return "value";
	}

	@Override
	protected BeanDefinition resolveHandlerBeanDefinition(String beanName, AnnotatedBeanDefinition beanDefinition,
			ResolvableType handlerBeanType, List<Annotation> annotationChain) {

		Class<?> handlerBeanClass = handlerBeanType.toClass();

		if (MessageSource.class.isAssignableFrom(handlerBeanClass)) {
			return beanDefinition;
		}

		Method method = null;
		if (Supplier.class.isAssignableFrom(handlerBeanClass)) {
			method = ClassUtils.SUPPLIER_GET_METHOD;
		}
		else if (ClassUtils.isKotlinFunction0(handlerBeanClass)) {
			method = ClassUtils.KOTLIN_FUNCTION_0_INVOKE_METHOD;
		}

		if (method != null) {
			return BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingMessageSource.class)
					.addPropertyReference("object", beanName)
					.addPropertyValue("method", method)
					.getBeanDefinition();
		}
		else {
			throw new IllegalArgumentException(
					"The '" + this.annotationType + "' on @Bean method level is allowed only for: " +
							MessageSource.class.getName() + ", or " + Supplier.class.getName() +
							(ClassUtils.KOTLIN_FUNCTION_0_CLASS != null
									? ", or " + ClassUtils.KOTLIN_FUNCTION_0_CLASS.getName()
									: "") + " beans");
		}
	}

	@Override
	protected BeanDefinition createEndpointBeanDefinition(ComponentDefinition handlerBeanDefinition,
			ComponentDefinition beanDefinition, List<Annotation> annotations) {

		String channelName = MessagingAnnotationUtils.resolveAttribute(annotations, AnnotationUtils.VALUE, String.class);
		Assert.hasText(channelName, "The channel ('value' attribute of @InboundChannelAdapter) can't be empty.");
		return BeanDefinitionBuilder.rootBeanDefinition(SourcePollingChannelAdapterFactoryBean.class)
				.addPropertyValue("outputChannelName", channelName)
				.addPropertyReference("source", handlerBeanDefinition.getName())
				.getBeanDefinition();
	}

	@Override
	public Object postProcess(Object bean, String beanName, Method method, List<Annotation> annotations) {
		String channelName =
				MessagingAnnotationUtils.resolveAttribute(annotations, AnnotationUtils.VALUE, String.class);
		Assert.hasText(channelName, "The channel ('value' attribute of @InboundChannelAdapter) can't be empty.");

		MessageSource<?> messageSource = createMessageSource(bean, beanName, method);

		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		adapter.setOutputChannelName(channelName);
		adapter.setSource(messageSource);
		Poller poller = MessagingAnnotationUtils.resolveAttribute(annotations, "poller", Poller.class);
		configurePollingEndpoint(adapter, poller);

		return adapter;
	}

	private MessageSource<?> createMessageSource(Object bean, String beanName, Method method) {
		MethodInvokingMessageSource methodInvokingMessageSource = new MethodInvokingMessageSource();
		methodInvokingMessageSource.setObject(bean);
		methodInvokingMessageSource.setMethod(method);
		String messageSourceBeanName = generateHandlerBeanName(beanName, method);
		getDefinitionRegistry().registerBeanDefinition(messageSourceBeanName,
				new RootBeanDefinition(MethodInvokingMessageSource.class, () -> methodInvokingMessageSource));
		return getBeanFactory().getBean(messageSourceBeanName, MessageSource.class);
	}

	@Override
	protected String generateHandlerBeanName(String originalBeanName, MergedAnnotations mergedAnnotations,
			@Nullable String methodName) {

		return super.generateHandlerBeanName(originalBeanName, mergedAnnotations, methodName)
				.replaceFirst(IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX + '$', ".source");
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		throw new UnsupportedOperationException();
	}

}
