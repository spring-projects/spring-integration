/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link Router @Router}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class RouterAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Router> {

	public RouterAnnotationPostProcessor(ConfigurableListableBeanFactory beanFactory) {
		super(beanFactory);
		this.messageHandlerAttributes.addAll(Arrays.<String>asList("defaultOutputChannel", "applySequence",
				"ignoreSendFailures", "resolutionRequired", "channelMappings", "prefix", "suffix"));
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		AbstractMessageRouter router;
		if (AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {
			Object target = this.resolveTargetBeanFromMethodWithBeanAnnotation(method);
			router = this.extractTypeIfPossible(target, AbstractMessageRouter.class);
			if (router == null) {
				if (target instanceof MessageHandler) {
					Assert.isTrue(this.routerAttributesProvided(annotations), "'defaultOutputChannel', 'applySequence', " +
							"'ignoreSendFailures', 'resolutionRequired', 'channelMappings', 'prefix' and 'suffix' " +
							"can be applied to 'AbstractMessageRouter' implementations, but target handler is: " +
							target.getClass());
					return (MessageHandler) target;
				}
				else {
					router = new MethodInvokingRouter(target);
				}
			}
			else {
				checkMessageHandlerAttributes(resolveTargetBeanName(method), annotations);
				return router;
			}
		}
		else {
			router = new MethodInvokingRouter(bean, method);
		}
		String defaultOutputChannelName = MessagingAnnotationUtils.resolveAttribute(annotations,
				"defaultOutputChannel", String.class);
		if (StringUtils.hasText(defaultOutputChannelName)) {
			router.setDefaultOutputChannelName(defaultOutputChannelName);
		}

		String applySequence = MessagingAnnotationUtils.resolveAttribute(annotations, "applySequence", String.class);
		if (StringUtils.hasText(applySequence)) {
			router.setApplySequence(Boolean.parseBoolean(this.beanFactory.resolveEmbeddedValue(applySequence)));
		}

		String ignoreSendFailures = MessagingAnnotationUtils.resolveAttribute(annotations, "ignoreSendFailures",
				String.class);
		if (StringUtils.hasText(ignoreSendFailures)) {
			router.setIgnoreSendFailures(Boolean.parseBoolean(this.beanFactory.resolveEmbeddedValue(ignoreSendFailures)));
		}

		if (this.routerAttributesProvided(annotations)) {

			MethodInvokingRouter methodInvokingRouter = (MethodInvokingRouter) router;

			String resolutionRequired = MessagingAnnotationUtils.resolveAttribute(annotations, "resolutionRequired",
					String.class);
			if (StringUtils.hasText(resolutionRequired)) {
				String resolutionRequiredValue = this.beanFactory.resolveEmbeddedValue(resolutionRequired);
				if (StringUtils.hasText(resolutionRequiredValue)) {
					methodInvokingRouter.setResolutionRequired(Boolean.parseBoolean(resolutionRequiredValue));
				}
			}

			String prefix = MessagingAnnotationUtils.resolveAttribute(annotations, "prefix", String.class);
			if (StringUtils.hasText(prefix)) {
				methodInvokingRouter.setPrefix(this.beanFactory.resolveEmbeddedValue(prefix));
			}

			String suffix = MessagingAnnotationUtils.resolveAttribute(annotations, "suffix", String.class);
			if (StringUtils.hasText(suffix)) {
				methodInvokingRouter.setSuffix(this.beanFactory.resolveEmbeddedValue(suffix));
			}

			String[] channelMappings = MessagingAnnotationUtils.resolveAttribute(annotations, "channelMappings",
					String[].class);
			if (!ObjectUtils.isEmpty(channelMappings)) {
				StringBuilder mappings = new StringBuilder();
				for (String channelMapping : channelMappings) {
					mappings.append(channelMapping).append("\n");
				}
				Properties properties = (Properties) this.conversionService.convert(mappings.toString(),
						TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Properties.class));
				methodInvokingRouter.replaceChannelMappings(properties);
			}

		}

		return router;
	}

	private boolean routerAttributesProvided(List<Annotation> annotations) {
		String defaultOutputChannel = MessagingAnnotationUtils.resolveAttribute(annotations, "defaultOutputChannel",
				String.class);
		String[] channelMappings = MessagingAnnotationUtils.resolveAttribute(annotations, "channelMappings",
				String[].class);
		String prefix = MessagingAnnotationUtils.resolveAttribute(annotations, "prefix", String.class);
		String suffix = MessagingAnnotationUtils.resolveAttribute(annotations, "suffix", String.class);
		String resolutionRequired = MessagingAnnotationUtils.resolveAttribute(annotations, "resolutionRequired",
				String.class);
		String applySequence = MessagingAnnotationUtils.resolveAttribute(annotations, "applySequence", String.class);
		String ignoreSendFailures = MessagingAnnotationUtils.resolveAttribute(annotations, "ignoreSendFailures",
				String.class);
		return StringUtils.hasText(defaultOutputChannel) || !ObjectUtils.isEmpty(channelMappings)
				|| StringUtils.hasText(prefix) || StringUtils.hasText(suffix) || StringUtils.hasText(resolutionRequired)
				|| StringUtils.hasText(applySequence) || StringUtils.hasText(ignoreSendFailures);
	}

}
