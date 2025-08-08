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
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.MessageHandler;

/**
 * Post-processor for Methods annotated with a
 * {@link org.springframework.integration.annotation.Transformer @Transformer}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class TransformerAnnotationPostProcessor
		extends AbstractMethodAnnotationPostProcessor<org.springframework.integration.annotation.Transformer> {

	public TransformerAnnotationPostProcessor() {
		this.messageHandlerAttributes.addAll(Arrays.asList("outputChannel", "adviceChain"));
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

		return BeanDefinitionBuilder.genericBeanDefinition(TransformerFactoryBean.class)
				.addPropertyValue("targetObject", targetObjectBeanDefinition)
				.getBeanDefinition();
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		Transformer transformer = new MethodInvokingTransformer(bean, method);
		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		setOutputChannelIfPresent(annotations, handler);
		return handler;
	}

}
