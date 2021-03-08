/*
 * Copyright 2014-2021 the original author or authors.
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
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.util.ClassUtils;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * Post-processor for Methods annotated with {@link InboundChannelAdapter @InboundChannelAdapter}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Oleg Zhurakousky
 *
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
		String channelName =
				MessagingAnnotationUtils.resolveAttribute(annotations, AnnotationUtils.VALUE, String.class);
		Assert.hasText(channelName, "The channel ('value' attribute of @InboundChannelAdapter) can't be empty.");

		MessageSource<?> messageSource;
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
		Poller[] pollers = MessagingAnnotationUtils.resolveAttribute(annotations, "poller", Poller[].class);
		configurePollingEndpoint(adapter, pollers);

		return adapter;
	}

	private MessageSource<?> createMessageSource(Object beanArg, String beanNameArg, Method methodArg) {
		MessageSource<?> messageSource = null;
		Object bean = beanArg;
		Method method = methodArg;
		String beanName = beanNameArg;
		if (AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {
			Object target = resolveTargetBeanFromMethodWithBeanAnnotation(method);
			Class<?> targetClass = target.getClass();
			Assert.isTrue(MessageSource.class.isAssignableFrom(targetClass) ||
							Supplier.class.isAssignableFrom(targetClass) ||
							ClassUtils.isKotlinFaction0(targetClass),
					() -> "The '" + this.annotationType + "' on @Bean method " + "level is allowed only for: " +
							MessageSource.class.getName() + " or " + Supplier.class.getName() +
							(ClassUtils.KOTLIN_FUNCTION_0_CLASS != null
									? " or " + ClassUtils.KOTLIN_FUNCTION_0_CLASS.getName()
									: "") + " beans");
			if (target instanceof MessageSource<?>) {
				messageSource = (MessageSource<?>) target;
			}
			else if (target instanceof Supplier<?>) {
				method = ClassUtils.SUPPLIER_GET_METHOD;
				bean = target;
				beanName += '.' + methodArg.getName();
			}
			else if (ClassUtils.KOTLIN_FUNCTION_0_INVOKE_METHOD != null) {
				method = ClassUtils.KOTLIN_FUNCTION_0_INVOKE_METHOD;
				bean = target;
				beanName += '.' + methodArg.getName();
			}
		}
		if (messageSource == null) {
			MethodInvokingMessageSource methodInvokingMessageSource = new MethodInvokingMessageSource();
			methodInvokingMessageSource.setObject(bean);
			methodInvokingMessageSource.setMethod(method);
			String messageSourceBeanName = generateHandlerBeanName(beanName, method);
			this.beanFactory.registerSingleton(messageSourceBeanName, methodInvokingMessageSource);
			messageSource = (MessageSource<?>) this.beanFactory
					.initializeBean(methodInvokingMessageSource, messageSourceBeanName);
			if (this.disposables != null) {
				this.disposables.add(methodInvokingMessageSource);
			}
		}
		return messageSource;
	}

	@Override
	protected String generateHandlerBeanName(String originalBeanName, Method method) {
		return super.generateHandlerBeanName(originalBeanName, method)
				.replaceFirst(IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX + '$', ".source");
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		throw new UnsupportedOperationException();
	}

}
