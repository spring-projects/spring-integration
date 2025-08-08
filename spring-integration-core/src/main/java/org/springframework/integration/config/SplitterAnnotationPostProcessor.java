/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
