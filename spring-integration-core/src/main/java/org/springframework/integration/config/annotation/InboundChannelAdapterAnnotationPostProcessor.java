/*
 * Copyright 2014-2016 the original author or authors.
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
import java.util.function.Supplier;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Post-processor for Methods annotated with {@link InboundChannelAdapter @InboundChannelAdapter}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 4.0
 */
public class InboundChannelAdapterAnnotationPostProcessor extends
		AbstractMethodAnnotationPostProcessor<InboundChannelAdapter> {

	public InboundChannelAdapterAnnotationPostProcessor(ConfigurableListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	@Override
	protected String getInputChannelAttribute() {
		return AnnotationUtils.VALUE;
	}

	@Override
	public Object postProcess(Object bean, String beanName, Method method, List<Annotation> annotations) {
		String channelName = MessagingAnnotationUtils.resolveAttribute(annotations, AnnotationUtils.VALUE, String.class);
		Assert.hasText(channelName, "The channel ('value' attribute of @InboundChannelAdapter) can't be empty.");

		MessageSource<?> messageSource = null;
		try {
			messageSource = createMessageSource(bean, beanName, method);
		}
		catch (NoSuchBeanDefinitionException e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Skipping endpoint creation; "
						+ e.getMessage()
						+ "; perhaps due to some '@Conditional' annotation.");
			}
			return null;
		}

		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		adapter.setOutputChannelName(channelName);
		adapter.setSource(messageSource);
		configurePollingEndpoint(adapter, annotations);

		return adapter;
	}

	private MessageSource<?> createMessageSource(Object bean, String beanName, Method method) {
		MessageSource<?> messageSource = null;
		if (AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {
			Object target = this.resolveTargetBeanFromMethodWithBeanAnnotation(method);
			Assert.isTrue(target instanceof MessageSource || target instanceof Supplier, "The '" + this.annotationType + "' on @Bean method " +
					"level is allowed only for: " + MessageSource.class.getName() + " or " + Supplier.class.getName() + " beans");
			if (target instanceof MessageSource<?>) {
				messageSource = (MessageSource<?>) target;
			}
			else {
				method = ReflectionUtils.findMethod(Supplier.class, "get");
				bean = target;
			}
		}
		if (messageSource == null) {
			MethodInvokingMessageSource methodInvokingMessageSource = new MethodInvokingMessageSource();
			methodInvokingMessageSource.setObject(bean);
			methodInvokingMessageSource.setMethod(method);
			String messageSourceBeanName = this.generateHandlerBeanName(beanName, method);
			this.beanFactory.registerSingleton(messageSourceBeanName, methodInvokingMessageSource);
			messageSource =  (MessageSource<?>) this.beanFactory.initializeBean(methodInvokingMessageSource, messageSourceBeanName);
		}
		return messageSource;
	}

	@Override
	protected String generateHandlerBeanName(String originalBeanName, Method method) {
		return super.generateHandlerBeanName(originalBeanName, method)
				.replaceFirst(IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX + "$", ".source");
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		throw new UnsupportedOperationException();
	}

}
