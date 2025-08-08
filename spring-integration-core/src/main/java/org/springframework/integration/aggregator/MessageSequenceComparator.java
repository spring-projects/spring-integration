/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.io.Serializable;
import java.util.Comparator;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 */
@SuppressWarnings("serial")
public class MessageSequenceComparator implements Comparator<Message<?>>, Serializable {

	@Override
	public int compare(Message<?> o1, Message<?> o2) {
		int sequenceNumber1 = new IntegrationMessageHeaderAccessor(o1).getSequenceNumber();
		int sequenceNumber2 = new IntegrationMessageHeaderAccessor(o2).getSequenceNumber();

		return Integer.compare(sequenceNumber1, sequenceNumber2);
	}

}
