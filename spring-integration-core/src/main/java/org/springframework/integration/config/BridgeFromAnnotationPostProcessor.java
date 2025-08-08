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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * Post-processor for the {@link BridgeFrom @BridgeFrom} annotation.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class BridgeFromAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<BridgeFrom> {

	@Override
	public String getInputChannelAttribute() {
		return AnnotationUtils.VALUE;
	}

	@Override
	public boolean supportsPojoMethod() {
		return false;
	}

	@Override
	public boolean shouldCreateEndpoint(MergedAnnotations mergedAnnotations, List<Annotation> annotations) {
		Assert.isTrue(super.shouldCreateEndpoint(mergedAnnotations, annotations),
				"'@BridgeFrom.value()' (inputChannelName) must not be empty");
		Assert.isTrue(!mergedAnnotations.isPresent(BridgeTo.class),
				"'@BridgeFrom' and '@BridgeTo' are mutually exclusive 'MessageChannel' '@Bean' method annotations");
		return true;
	}

	@Override
	protected BeanDefinition resolveHandlerBeanDefinition(String beanName, AnnotatedBeanDefinition beanDefinition,
			ResolvableType handlerBeanType, List<Annotation> annotationChain) {

		return BeanDefinitionBuilder.genericBeanDefinition(BridgeHandler.class)
				.addPropertyReference("outputChannel", beanName)
				.getBeanDefinition();
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		throw new UnsupportedOperationException();
	}

}
