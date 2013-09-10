/*
 * Copyright 2002-2008 the original author or authors.
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
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link ServiceActivator @ServiceActivator}.
 *
 * @author Mark Fisher
 */
public class ServiceActivatorAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<ServiceActivator> {

	public ServiceActivatorAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, ServiceActivator annotation) {
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(bean, method);
		String outputChannelName = annotation.outputChannel();
		if (StringUtils.hasText(outputChannelName)) {
			serviceActivator.setOutputChannel(this.channelResolver.resolveDestination(outputChannelName));
		}
		return serviceActivator;
	}

}
