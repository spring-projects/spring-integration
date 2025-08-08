/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.springframework.integration.store.MessageGroup;

/**
 * A processor for <i>correlated</i> groups of messages.
 *
 * @author Iwein Fuld
 * @see org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler
 */
@FunctionalInterface
public interface MessageGroupProcessor {

	/**
	 * Process the given MessageGroup. Implementations are free to return as few or as many messages based on the
	 * invocation as needed. For example an aggregating processor will return only a single message representing the
	 * group, while a resequencing processor will return all messages whose preceding sequence has been satisfied.
	 * <p>
	 * If a multiple messages are returned the return value must be a Collection&lt;Message&gt;.
	 *
	 * @param group The message group.
	 * @return The result of processing the group.
	 */
	Object processMessageGroup(MessageGroup group);

}
