/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Post-processor for the {@link BridgeTo @BridgeTo} annotation.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class BridgeToAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<BridgeTo> {

	@Override
	public boolean supportsPojoMethod() {
		return false;
	}

	@Override
	public boolean shouldCreateEndpoint(MergedAnnotations mergedAnnotations, List<Annotation> annotations) {
		Assert.isTrue(!mergedAnnotations.isPresent(BridgeFrom.class),
				"'@BridgeFrom' and '@BridgeTo' are mutually exclusive 'MessageChannel' '@Bean' method annotations");
		return true;
	}

	@Override
	protected BeanDefinition resolveHandlerBeanDefinition(String beanName, AnnotatedBeanDefinition beanDefinition,
			ResolvableType handlerBeanType, List<Annotation> annotationChain) {

		GenericBeanDefinition bridgeHandlerBeanDefinition = new GenericBeanDefinition();
		bridgeHandlerBeanDefinition.setBeanClass(BridgeHandler.class);
		String outputChannelName = MessagingAnnotationUtils.resolveAttribute(annotationChain, "value", String.class);
		if (StringUtils.hasText(outputChannelName)) {
			bridgeHandlerBeanDefinition.getPropertyValues()
					.addPropertyValue("outputChannel", new RuntimeBeanReference(outputChannelName));
		}
		return bridgeHandlerBeanDefinition;
	}

	@Override
	protected BeanDefinition createEndpointBeanDefinition(ComponentDefinition handlerBeanDefinition,
			ComponentDefinition beanDefinition, List<Annotation> annotations) {

		return BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class)
				.addPropertyReference("handler", handlerBeanDefinition.getName())
				.addPropertyReference("inputChannel", beanDefinition.getName())
				.getBeanDefinition();
	}

	@Override
	protected AbstractEndpoint createEndpoint(MessageHandler handler, Method method, List<Annotation> annotations) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		throw new UnsupportedOperationException();
	}

}
