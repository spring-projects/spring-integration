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
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link Splitter @Splitter}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class SplitterAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Splitter> {

	private static final String APPLY_SEQUENCE_ATTR = "applySequence";

	public SplitterAnnotationPostProcessor() {
		this.messageHandlerAttributes.addAll(Arrays.asList("outputChannel", APPLY_SEQUENCE_ATTR, "adviceChain"));
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

		BeanDefinitionBuilder splitterBeanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition(SplitterFactoryBean.class)
						.addPropertyValue("targetObject", targetObjectBeanDefinition);

		String applySequence = MessagingAnnotationUtils.resolveAttribute(annotations, APPLY_SEQUENCE_ATTR, String.class);
		if (StringUtils.hasText(applySequence)) {
			splitterBeanDefinition.addPropertyValue(APPLY_SEQUENCE_ATTR, applySequence);
		}
		return splitterBeanDefinition.getBeanDefinition();
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		AbstractMessageSplitter splitter = new MethodInvokingSplitter(bean, method);

		String applySequence = MessagingAnnotationUtils.resolveAttribute(annotations, APPLY_SEQUENCE_ATTR, String.class);
		if (StringUtils.hasText(applySequence)) {
			splitter.setApplySequence(resolveAttributeToBoolean(applySequence));
		}

		setOutputChannelIfPresent(annotations, splitter);
		return splitter;
	}

}
