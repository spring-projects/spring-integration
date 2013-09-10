/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link Transformer @Transformer}.
 *
 * @author Mark Fisher
 */
public class TransformerAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Transformer> {

	public TransformerAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, Transformer annotation) {
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(bean, method);
		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		String outputChannelName = annotation.outputChannel();
		if (StringUtils.hasText(outputChannelName)) {
			handler.setOutputChannel(this.channelResolver.resolveDestination(outputChannelName));
		}
		return handler;
	}

}
