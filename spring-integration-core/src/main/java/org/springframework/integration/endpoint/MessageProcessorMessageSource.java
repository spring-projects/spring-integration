/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;

/**
 * The {@link org.springframework.integration.core.MessageSource} strategy implementation
 * to produce a {@link org.springframework.messaging.Message} from underlying
 * {@linkplain #messageProcessor} for polling endpoints.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Jiandong Ma
 *
 * @since 5.0
 */
public class MessageProcessorMessageSource extends AbstractMessageSource<Object> {

	/**
	 * A fake message since the {@link MessageProcessor#processMessage(Message)} requires a non-null.
 	 */
	static final Message<Object> FAKE_MESSAGE = MutableMessageBuilder.withPayload(new Object(), false).build();

	private final MessageProcessor<?> messageProcessor;

	public MessageProcessorMessageSource(MessageProcessor<?> messageProcessor) {
		this.messageProcessor = messageProcessor;
	}

	@Override
	public String getComponentType() {
		return "inbound-channel-adapter";
	}

	@Override
	protected @Nullable Object doReceive() {
		return this.messageProcessor.processMessage(FAKE_MESSAGE);
	}

}
