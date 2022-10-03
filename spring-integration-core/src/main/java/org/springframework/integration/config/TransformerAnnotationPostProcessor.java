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
