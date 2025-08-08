/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.util.UUID;

import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.store.MessageStore;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Transformer that stores a Message and returns a new Message whose payload
 * is the id of the stored Message.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ClaimCheckInTransformer extends AbstractTransformer implements IntegrationPattern {

	private final MessageStore messageStore;

	/**
	 * Create a claim check-in transformer that will delegate to the provided MessageStore.
	 *
	 * @param messageStore The message store.
	 */
	public ClaimCheckInTransformer(MessageStore messageStore) {
		Assert.notNull(messageStore, "MessageStore must not be null");
		this.messageStore = messageStore;
	}

	@Override
	public String getComponentType() {
		return "claim-check-in";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.claim_check_in;
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Assert.notNull(message, "message must not be null");
		UUID id = message.getHeaders().getId();
		Assert.notNull(id, "ID header must not be null");
		this.messageStore.addMessage(message);
		return id;
	}

}
