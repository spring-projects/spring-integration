/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.util.UUID;

import org.springframework.core.log.LogMessage;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Transformer that accepts a Message whose payload is a UUID and retrieves the Message associated
 * with that id from a MessageStore if available. An Exception will be thrown if no Message with
 * that ID can be retrieved from the given MessageStore.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Nick Spacek
 * @author Artem Bilan
 * @author Trung Pham
 *
 * @since 2.0
 */
public class ClaimCheckOutTransformer extends AbstractTransformer implements IntegrationPattern {

	private final MessageStore messageStore;

	private volatile boolean removeMessage = false;

	/**
	 * Create a claim check-out transformer that will delegate to the provided MessageStore.
	 *
	 * @param messageStore The message store.
	 */
	public ClaimCheckOutTransformer(MessageStore messageStore) {
		Assert.notNull(messageStore, "MessageStore must not be null");
		this.messageStore = messageStore;
	}

	public void setRemoveMessage(boolean removeMessage) {
		this.removeMessage = removeMessage;
	}

	@Override
	public String getComponentType() {
		return "claim-check-out";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.claim_check_out;
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Assert.notNull(message, "message must not be null");
		Assert.isTrue(message.getPayload() instanceof UUID, "payload must be a UUID");
		UUID id = (UUID) message.getPayload();
		Message<?> retrievedMessage;
		if (this.removeMessage) {
			retrievedMessage = this.messageStore.removeMessage(id);
			logger.debug(LogMessage.format("Removed Message with claim-check '%s' from the MessageStore.", id));
		}
		else {
			retrievedMessage = this.messageStore.getMessage(id);
		}
		Assert.notNull(retrievedMessage,
				() -> "unable to locate Message for ID: " + id + " within MessageStore [" + this.messageStore + "]");
		AbstractIntegrationMessageBuilder<?> responseBuilder = getMessageBuilderFactory().fromMessage(retrievedMessage);
		// headers on the 'current' message take precedence
		responseBuilder.copyHeaders(message.getHeaders());
		return responseBuilder.build();
	}

}
