/*
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.handler;

import java.util.List;

import org.jspecify.annotations.Nullable;

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

	@SuppressWarnings("NullAway.Init")
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
	public @Nullable Object processMessage(Message<?> message) {
		String command = message.getPayload().toString();
		@SuppressWarnings("unchecked")
		List<Object> arguments =
				message.getHeaders().get(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.class);
		Class<?>[] parameterTypes = {};
		if (!CollectionUtils.isEmpty(arguments)) {
			parameterTypes = new Class<?>[arguments.size()];
			for (int i = 0; i < arguments.size(); i++) {
				parameterTypes[i] = arguments.get(i).getClass();
			}
		}
		Expression commandExpression = this.controlBusCommandRegistry.getExpressionForCommand(command, parameterTypes);
		return evaluateExpression(commandExpression, arguments);
	}

}
