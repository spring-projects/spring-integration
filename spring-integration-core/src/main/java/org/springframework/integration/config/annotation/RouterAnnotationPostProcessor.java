/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.annotation.Router;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link Router @Router}.
 *
 * @author Mark Fisher
 */
public class RouterAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Router> {

	public RouterAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, Router annotation) {
		MethodInvokingRouter router = new MethodInvokingRouter(bean, method);
		router.setBeanFactory(this.beanFactory);
		String defaultOutputChannelName = annotation.defaultOutputChannel();
		if (StringUtils.hasText(defaultOutputChannelName)) {
			MessageChannel defaultOutputChannel = this.channelResolver.resolveDestination(defaultOutputChannelName);
			Assert.notNull(defaultOutputChannel, "unable to resolve defaultOutputChannel '" + defaultOutputChannelName + "'");
			router.setDefaultOutputChannel(defaultOutputChannel);
		}
		return router;
	}

}
