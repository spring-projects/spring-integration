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

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.DefaultServiceInvoker;
import org.springframework.integration.endpoint.ServiceActivatorEndpoint;
import org.springframework.integration.endpoint.ServiceInvoker;

/**
 * Post-processor for Methods annotated with {@link ServiceActivator @ServiceActivator}.
 * 
 * @author Mark Fisher
 */
public class ServiceActivatorAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<ServiceActivator> {

	public ServiceActivatorAnnotationPostProcessor(MessageBus messageBus) {
		super(messageBus);
	}


	@Override
	protected Object createMethodInvokingAdapter(Object bean, Method method, ServiceActivator annotation) {
		return new DefaultServiceInvoker(bean, method);
	}

	@Override
	protected AbstractEndpoint createEndpoint(Object adapter) {
		if (adapter instanceof ServiceInvoker) {
			return new ServiceActivatorEndpoint((ServiceInvoker) adapter);
		}
		return null;
	}

}
