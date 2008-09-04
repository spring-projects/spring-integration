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

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.router.MethodInvokingChannelResolver;
import org.springframework.integration.router.RouterEndpoint;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link Router @Router}.
 *
 * @author Mark Fisher
 */
public class RouterAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Router> {

	public RouterAnnotationPostProcessor(MessageBus messageBus) {
		super(messageBus);
	}


	@Override
	protected Object createMethodInvokingAdapter(Object bean, Method method, Router annotation) {
		return new MethodInvokingChannelResolver(bean, method);
	}

	@Override
	protected AbstractEndpoint createEndpoint(Object adapter) {
		if (adapter instanceof MethodInvokingChannelResolver) {
			return new RouterEndpoint((MethodInvokingChannelResolver) adapter);
		}
		return null;
	}

	@Override
	protected void configureEndpoint(AbstractEndpoint endpoint, Router annotation, Poller pollerAnnotation) {
		super.configureEndpoint(endpoint, annotation, pollerAnnotation);
		String defaultOutputChannelName = annotation.defaultOutputChannel();
		if (StringUtils.hasText(defaultOutputChannelName)) {
			MessageChannel defaultOutputChannel = this.getChannelRegistry().lookupChannel(defaultOutputChannelName);
			if (defaultOutputChannel == null) {
				throw new ConfigurationException("unable to resolve defaultOutputChannel '" + defaultOutputChannelName + "'");
			}
			((RouterEndpoint) endpoint).setDefaultOutputChannel(defaultOutputChannel);
		}
	}

}
