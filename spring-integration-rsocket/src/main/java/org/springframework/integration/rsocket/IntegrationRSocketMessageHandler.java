/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.rsocket;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.reactive.SyncHandlerMethodArgumentResolver;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.ReflectionUtils;

/**
 * The {@link RSocketMessageHandler} extension for Spring Integration needs.
 * <p>
 * This class adds an {@link IntegrationRSocketEndpoint} beans detection and registration functionality.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see RSocketMessageHandler
 */
class IntegrationRSocketMessageHandler extends RSocketMessageHandler {

	private static final Method HANDLE_MESSAGE_METHOD =
			ReflectionUtils.findMethod(ReactiveMessageHandler.class, "handleMessage", Message.class);

	protected final boolean messageMappingCompatible; // NOSONAR final

	IntegrationRSocketMessageHandler() {
		this(false);
	}

	IntegrationRSocketMessageHandler(boolean messageMappingCompatible) {
		this.messageMappingCompatible = messageMappingCompatible;
		if (!this.messageMappingCompatible) {
			setHandlerPredicate((clazz) -> false);
		}
	}

	public boolean detectEndpoints() {
		ApplicationContext applicationContext = getApplicationContext();
		if (applicationContext != null && getHandlerMethods().isEmpty()) {
			return applicationContext
					.getBeansOfType(IntegrationRSocketEndpoint.class)
					.values()
					.stream()
					.peek(this::addEndpoint)
					.count() > 0;
		}
		else {
			return false;
		}
	}

	public void addEndpoint(IntegrationRSocketEndpoint endpoint) {
		registerHandlerMethod(endpoint, HANDLE_MESSAGE_METHOD,
				new CompositeMessageCondition(
						RSocketFrameTypeMessageCondition.REQUEST_CONDITION,
						new DestinationPatternsMessageCondition(endpoint.getPath(), getRouteMatcher()))); // NOSONAR
	}

	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		if (this.messageMappingCompatible) {
			// Add argument resolver before parent initializes argument resolution
			getArgumentResolverConfigurer().addCustomResolver(new MessageHandlerMethodArgumentResolver());
			return super.initArgumentResolvers();
		}
		else {
			return Collections.singletonList(new MessageHandlerMethodArgumentResolver());
		}
	}

	protected static final class MessageHandlerMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return Message.class.equals(parameter.getParameterType());
		}

		@Override
		public Object resolveArgumentValue(MethodParameter parameter, Message<?> message) {
			return message;
		}

	}

}
