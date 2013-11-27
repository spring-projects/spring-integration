/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.router;

import java.lang.reflect.Method;

import org.springframework.integration.annotation.Router;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;

/**
 * A Message Router that invokes the specified method on the given object. The
 * method's return value may be a single MessageChannel instance, a single
 * String to be interpreted as a channel name, or a Collection (or Array) of
 * either type. If the method returns channel names, then a
 * {@link org.springframework.messaging.core.DestinationResolver} is required.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MethodInvokingRouter extends AbstractMessageProcessingRouter {

	public MethodInvokingRouter(Object object, Method method) {
		super(new MethodInvokingMessageProcessor<Object>(object, method));
	}

	public MethodInvokingRouter(Object object, String methodName) {
		super(new MethodInvokingMessageProcessor<Object>(object, methodName));
	}

	public MethodInvokingRouter(Object object) {
		super(object instanceof MessageProcessor<?> ? (MessageProcessor<?>) object :
				new MethodInvokingMessageProcessor<Object>(object, Router.class));
	}

}
