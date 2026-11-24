/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
