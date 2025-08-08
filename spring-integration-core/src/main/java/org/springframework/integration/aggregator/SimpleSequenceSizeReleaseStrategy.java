/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.springframework.integration.store.MessageGroup;

/**
 * An implementation of {@link ReleaseStrategy} that simply compares the current size of
 * the message list to the expected 'sequenceSize'. It does not support releasing partial
 * sequences. Correlating message handlers using this strategy do not check for duplicate
 * sequence numbers.
 * @author Gary Russell
 * @since 4.3.4
 *
 */
public class SimpleSequenceSizeReleaseStrategy implements ReleaseStrategy {

	@Override
	public boolean canRelease(MessageGroup group) {
		return group.getSequenceSize() == group.size();
	}

}
