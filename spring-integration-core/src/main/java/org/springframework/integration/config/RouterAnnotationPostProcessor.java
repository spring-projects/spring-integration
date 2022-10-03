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
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.MessageHandler;
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

	public RouterAnnotationPostProcessor() {
		this.messageHandlerAttributes.addAll(Arrays.asList("defaultOutputChannel", "applySequence",
				"ignoreSendFailures", "resolutionRequired", "channelMappings", "prefix", "suffix"));
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

		BeanDefinition routerBeanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition(RouterFactoryBean.class)
						.addPropertyValue("targetObject", targetObjectBeanDefinition)
						.getBeanDefinition();

		new BeanDefinitionPropertiesMapper(routerBeanDefinition, annotations)
				.setPropertyValue("applySequence")
				.setPropertyValue("ignoreSendFailures")
				.setPropertyValue("resolutionRequired")
				.setPropertyValue("prefix")
				.setPropertyValue("suffix");

		String[] channelMappings = MessagingAnnotationUtils.resolveAttribute(annotations, "channelMappings",
				String[].class);
		if (!ObjectUtils.isEmpty(channelMappings)) {
			Map<String, String> mappings =
					Arrays.stream(channelMappings)
							.map((mapping) -> {
								String[] keyValue = mapping.split("=");
								return Map.entry(keyValue[0], keyValue[1]);
							})
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			routerBeanDefinition.getPropertyValues()
					.addPropertyValue("channelMappings", mappings);
		}

		return routerBeanDefinition;
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		AbstractMessageRouter router = new MethodInvokingRouter(bean, method);
		String defaultOutputChannelName =
				MessagingAnnotationUtils.resolveAttribute(annotations, "defaultOutputChannel", String.class);
		if (StringUtils.hasText(defaultOutputChannelName)) {
			router.setDefaultOutputChannelName(defaultOutputChannelName);
		}

		String applySequence = MessagingAnnotationUtils.resolveAttribute(annotations, "applySequence", String.class);
		if (StringUtils.hasText(applySequence)) {
			router.setApplySequence(resolveAttributeToBoolean(applySequence));
		}

		String ignoreSendFailures = MessagingAnnotationUtils.resolveAttribute(annotations, "ignoreSendFailures",
				String.class);
		if (StringUtils.hasText(ignoreSendFailures)) {
			router.setIgnoreSendFailures(resolveAttributeToBoolean(ignoreSendFailures));
		}

		routerAttributes(annotations, router);

		return router;
	}

	private void routerAttributes(List<Annotation> annotations, AbstractMessageRouter router) {
		if (routerAttributesProvided(annotations)) {

			MethodInvokingRouter methodInvokingRouter = (MethodInvokingRouter) router;

			String resolutionRequired = MessagingAnnotationUtils.resolveAttribute(annotations, "resolutionRequired",
					String.class);
			if (StringUtils.hasText(resolutionRequired)) {
				methodInvokingRouter.setResolutionRequired(resolveAttributeToBoolean(resolutionRequired));
			}

			ConfigurableListableBeanFactory beanFactory = getBeanFactory();

			String prefix = MessagingAnnotationUtils.resolveAttribute(annotations, "prefix", String.class);
			if (StringUtils.hasText(prefix)) {
				methodInvokingRouter.setPrefix(beanFactory.resolveEmbeddedValue(prefix));
			}

			String suffix = MessagingAnnotationUtils.resolveAttribute(annotations, "suffix", String.class);
			if (StringUtils.hasText(suffix)) {
				methodInvokingRouter.setSuffix(beanFactory.resolveEmbeddedValue(suffix));
			}

			String[] channelMappings = MessagingAnnotationUtils.resolveAttribute(annotations, "channelMappings",
					String[].class);
			if (!ObjectUtils.isEmpty(channelMappings)) {
				StringBuilder mappings = new StringBuilder();
				for (String channelMapping : channelMappings) {
					mappings.append(channelMapping).append("\n");
				}
				Properties properties = (Properties) getConversionService().convert(mappings.toString(),
						TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Properties.class));
				methodInvokingRouter.replaceChannelMappings(properties);
			}

		}
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
		return StringUtils.hasText(defaultOutputChannel) || !ObjectUtils.isEmpty(channelMappings) // NOSONAR complexity
				|| StringUtils.hasText(prefix) || StringUtils.hasText(suffix) || StringUtils.hasText(resolutionRequired)
				|| StringUtils.hasText(applySequence) || StringUtils.hasText(ignoreSendFailures);
	}

}
