/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel;

import java.util.Comparator;

import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 */
public class MessagePayloadTestComparator implements Comparator<Message<Comparable<Object>>> {

	public int compare(Message<Comparable<Object>> message1, Message<Comparable<Object>> message2) {
		return message1.getPayload().compareTo(message2.getPayload());
	}

}
