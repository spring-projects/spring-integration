/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;

import java.util.*;

/**
 * This class implements all the strategy interfaces needed for a default resequencer.
 *
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ResequencingMessageGroupProcessor implements MessageGroupProcessor {

	private volatile Comparator<Message<?>> comparator = new SequenceNumberComparator();

	/**
	 * A comparator to use to order messages before processing. The default is to order by sequence number.
	 *
	 * @param comparator the comparator to use to order messages
	 */
	public void setComparator(Comparator<Message<?>> comparator) {
		this.comparator = comparator;
	}
	
	public Object processMessageGroup(MessageGroup group) {
		Collection<Message<?>> messages = group.getMessages();

		if (messages.size() > 0) {
			List<Message<?>> sorted = new ArrayList<Message<?>>(messages);
			Collections.sort(sorted, this.comparator);
			ArrayList<Message<?>> partialSequence = new ArrayList<Message<?>>();
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
		return message.getHeaders().getSequenceNumber();
	}
}
