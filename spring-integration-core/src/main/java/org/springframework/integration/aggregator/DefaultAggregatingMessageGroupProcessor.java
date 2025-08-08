/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * This implementation of MessageGroupProcessor will take the messages from the
 * MessageGroup and pass them on in a single message with a Collection as a payload.
 *
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @since 2.0
 */
public class DefaultAggregatingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor {

	@Override
	protected final Object aggregatePayloads(MessageGroup group, Map<String, Object> headers) {
		Collection<Message<?>> messages = group.getMessages();
		Assert.notEmpty(messages, this.getClass().getSimpleName() + " cannot process empty message groups");
		List<Object> payloads = new ArrayList<Object>(messages.size());
		for (Message<?> message : messages) {
			payloads.add(message.getPayload());
		}
		return payloads;
	}

}
