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
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link ServiceActivator @ServiceActivator}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ServiceActivatorAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<ServiceActivator> {

	public ServiceActivatorAnnotationPostProcessor(ListableBeanFactory beanFactory, Environment environment) {
		super(beanFactory, environment);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		AbstractReplyProducingMessageHandler serviceActivator;
		if (AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {
			final Object target = this.resolveTargetBeanFromMethodWithBeanAnnotation(method);
			serviceActivator = this.extractTypeIfPossible(target, AbstractReplyProducingMessageHandler.class);
			if (serviceActivator == null) {
				if (target instanceof MessageHandler) {
					/*
			 		 * Return a reply-producing message handler so that we still get 'produced no reply' messages
			 		 * and the super class will inject the advice chain to advise the handler method if needed.
			 		 */
					return new AbstractReplyProducingMessageHandler() {

						@Override
						protected Object handleRequestMessage(Message<?> requestMessage) {

							((MessageHandler) target).handleMessage(requestMessage);
							return null;
						}
					};
				}
				else {
					serviceActivator = new ServiceActivatingHandler(target);
				}
			}
			else {
				return (MessageHandler) target;
			}
		}
		else {
			serviceActivator = new ServiceActivatingHandler(bean, method);
		}

		String requiresReply = MessagingAnnotationUtils.resolveAttribute(annotations, "requiresReply", String.class);
		if (StringUtils.hasText(requiresReply)) {
			serviceActivator.setRequiresReply(Boolean.parseBoolean(this.environment.resolvePlaceholders(requiresReply)));
		}

		this.setOutputChannelIfPresent(annotations, serviceActivator);
		return serviceActivator;
	}

}
