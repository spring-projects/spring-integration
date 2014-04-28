/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.messaging.MessageHandler;

/**
 * Post-processor for Methods annotated with {@link Transformer @Transformer}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class TransformerAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Transformer> {

	public TransformerAnnotationPostProcessor(ListableBeanFactory beanFactory, Environment environment) {
		super(beanFactory, environment);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		org.springframework.integration.transformer.Transformer transformer;
		if (AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {
			Object target = this.resolveTargetBeanFromMethodWithBeanAnnotation(method);
			transformer = this.extractTypeIfPossible(target, org.springframework.integration.transformer.Transformer.class);
			if (transformer == null) {
				if (this.extractTypeIfPossible(target, AbstractReplyProducingMessageHandler.class) != null) {
					return (MessageHandler) target;
				}
				transformer = new MethodInvokingTransformer(target);
			}
		}
		else {
			transformer = new MethodInvokingTransformer(bean, method);
		}

		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		this.setOutputChannelIfPresent(annotations, handler);
		return handler;
	}

}
