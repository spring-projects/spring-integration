/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.integration.handler;

import java.util.List;

import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.support.management.ControlBusCommandRegistry;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * A MessageProcessor implementation that expects a Control Bus command as a request message.
 * When processing, it evaluates a SpEL expression associated with requested command,
 * essentially target bean method invocation.
 * <p>
 * The arguments for the command must be provided
 * in the {@link IntegrationMessageHeaderAccessor#CONTROL_BUS_ARGUMENTS} message header.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 */
public class ControlBusMessageProcessor extends AbstractMessageProcessor<Object>
		implements IntegrationPattern {

	private ControlBusCommandRegistry controlBusCommandRegistry;

	public ControlBusMessageProcessor() {

	}

	/**
	 * Create an instance based on the provided {@link ControlBusCommandRegistry}.
	 * @param controlBusCommandRegistry the {@link ControlBusCommandRegistry} with commands to execute.
	 */
	public ControlBusMessageProcessor(ControlBusCommandRegistry controlBusCommandRegistry) {
		this.controlBusCommandRegistry = controlBusCommandRegistry;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.control_bus;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.controlBusCommandRegistry == null) {
			this.controlBusCommandRegistry = getBeanFactory().getBean(ControlBusCommandRegistry.class);
		}
	}

	@Override
	public Object processMessage(Message<?> message) {
		String command = message.getPayload().toString();
		@SuppressWarnings("unchecked")
		List<Object> arguments =
				message.getHeaders().get(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.class);
		Class<?>[] parameterTypes = new Class<?>[0];
		if (!CollectionUtils.isEmpty(arguments)) {
			parameterTypes =
					arguments.stream()
							.map(Object::getClass)
							.toArray(Class<?>[]::new);
		}
		Expression commandExpression = this.controlBusCommandRegistry.getExpressionForCommand(command, parameterTypes);
		return evaluateExpression(commandExpression, arguments);
	}

}
