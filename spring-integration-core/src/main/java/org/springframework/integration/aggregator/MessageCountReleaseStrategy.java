/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.springframework.integration.store.MessageGroup;

/**
 * A {@link ReleaseStrategy} that releases only the first {@code n} messages, where {@code n} is a threshold.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 */
public class MessageCountReleaseStrategy implements ReleaseStrategy {

	private final int threshold;

	/**
	 * Convenient constructor is only one message is required (threshold=1).
	 */
	public MessageCountReleaseStrategy() {
		this(1);
	}

	/**
	 * Construct an instance based on the provided threshold.
	 * @param threshold the number of messages to accept before releasing
	 */
	public MessageCountReleaseStrategy(int threshold) {
		this.threshold = threshold;
	}

	/**
	 * Release the group if it has more messages than the threshold and has not previously been released.
	 * It is possible that more messages than the threshold could be released, but only if multiple consumers
	 * receive messages from the same group concurrently.
	 */
	@Override
	public boolean canRelease(MessageGroup group) {
		return group.size() >= this.threshold;
	}

}
