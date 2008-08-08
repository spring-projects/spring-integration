package org.springframework.integration.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;

public class CollectionSplitter {

	@SuppressWarnings("unchecked")
	@Splitter
	public final List<Message> split(Message message) {
		List<Message> splitMessages = new ArrayList<Message>();
		for (Object payload : ((Collection) message.getPayload())) {
			splitMessages.add(new GenericMessage(payload));
		}
		return splitMessages;
	}
}
