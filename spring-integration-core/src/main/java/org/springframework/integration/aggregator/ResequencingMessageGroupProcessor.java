/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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

	public Object processMessageGroup(MessageGroup group) {
		Collection<Message<?>> messages = group.getMessages();

		if (!messages.isEmpty()) {
			List<Message<?>> sorted = new ArrayList<>(messages);
			sorted.sort(this.comparator);
			ArrayList<Message<?>> partialSequence = new ArrayList<>();
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
