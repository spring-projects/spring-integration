/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.scripting;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * The {@link org.springframework.integration.core.MessageSource} strategy implementation
 * to produce a {@link org.springframework.messaging.Message} from underlying
 * {@linkplain #scriptMessageProcessor} for polling endpoints.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class ScriptExecutingMessageSource extends AbstractMessageSource<Object> {

	private static final Message<byte[]> EMPTY_MESSAGE = MessageBuilder.withPayload(new byte[0]).build();

	private final AbstractScriptExecutingMessageProcessor<?> scriptMessageProcessor;

	public ScriptExecutingMessageSource(AbstractScriptExecutingMessageProcessor<?> scriptMessageProcessor) {
		this.scriptMessageProcessor = scriptMessageProcessor;
	}

	@Override
	public String getComponentType() {
		return "inbound-channel-adapter";
	}

	@Override
	protected @Nullable Object doReceive() {
		return this.scriptMessageProcessor.processMessage(EMPTY_MESSAGE);
	}

}
