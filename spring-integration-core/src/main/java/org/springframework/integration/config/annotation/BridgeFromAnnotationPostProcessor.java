/*
 * Copyright 2014 the original author or authors.
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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Post-processor for the {@link BridgeFrom @BridgeFrom} annotation.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class BridgeFromAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<BridgeFrom> {

	public BridgeFromAnnotationPostProcessor(ListableBeanFactory beanFactory, Environment environment) {
		super(beanFactory, environment);
		this.inputChannelAttribute = "value";
	}

	@Override
	public boolean shouldCreateEndpoint(Method method, List<Annotation> annotations) {
		boolean isBean = AnnotatedElementUtils.isAnnotated(method, Bean.class.getName());
		Assert.isTrue(isBean, "'@BridgeFrom' is eligible only for '@Bean' methods");

		boolean isMessageChannelBean = MessageChannel.class.isAssignableFrom(method.getReturnType());
		Assert.isTrue(isMessageChannelBean, "'@BridgeFrom' is eligible only for 'MessageChannel' '@Bean' methods");

		String channel = MessagingAnnotationUtils.resolveAttribute(annotations, "value", String.class);
		Assert.isTrue(StringUtils.hasText(channel), "'@BridgeFrom.value()' (inputChannelName) must not be empty");

		return true;
	}

	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		BridgeHandler handler = new BridgeHandler();
		String outputChannelName = null;
		String[] names = AnnotationUtils.getAnnotation(method, Bean.class).name();
		if (!ObjectUtils.isEmpty(names)) {
			outputChannelName = names[0];
		}
		if (!StringUtils.hasText(outputChannelName)) {
			outputChannelName = method.getName();
		}
		handler.setOutputChannelName(outputChannelName);
		return handler;
	}

}
