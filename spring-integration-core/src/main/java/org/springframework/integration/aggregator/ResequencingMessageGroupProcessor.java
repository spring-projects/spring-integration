/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;

/**
 * This class implements all the strategy interfaces needed for a default resequencer.
 *
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 2.0
 */
public class ResequencingMessageGroupProcessor implements MessageGroupProcessor {

	private final Comparator<Message<?>> comparator = new MessageSequenceComparator();

	@Nullable
	public Object processMessageGroup(MessageGroup group) {
		Collection<Message<?>> messages = group.getMessages();

		if (!messages.isEmpty()) {
			List<Message<?>> sorted = new ArrayList<>(messages);
			sorted.sort(this.comparator);
			ArrayList<Message<?>> partialSequence = new ArrayList<>(messages.size());
			int previousSequence = extractSequenceNumber(sorted.get(0));
			int currentSequence = previousSequence;
			for (Message<?> message : sorted) {
				previousSequence = currentSequence;
				currentSequence = extractSequenceNumber(message);
				if (currentSequence - 1 > previousSequence) {
					//there is a gap in the sequence here
					break;
				}
				partialSequence.add(message);
			}

			return partialSequence;
		}
		return null;
	}

	private Integer extractSequenceNumber(Message<?> message) {
		return StaticMessageHeaderAccessor.getSequenceNumber(message);
	}

}
