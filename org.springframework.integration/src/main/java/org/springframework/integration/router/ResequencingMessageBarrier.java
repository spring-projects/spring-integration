/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.springframework.integration.message.Message;

/**
 * MessageBarrier implementation for resequencing. It can either
 * release partial sequences as messages arrive, or the full sequence.
 * @author Marius Bogoevici
 */
public class ResequencingMessageBarrier extends AbstractMessageBarrier {

	private int lastReleasedSequenceNumber;

	private final Comparator<Message<?>> resequencingComparator;

	private final boolean releasePartialSequences;


	/**
	 * @param releasePartialSequences specifies whether partial sequences should
	 * be released as they arrive, or the resequencer
	 */
	public ResequencingMessageBarrier(boolean releasePartialSequences) {
		this.resequencingComparator = new Comparator<Message<?>>() {
			public int compare(Message<?> m1, Message<?> m2) {
				return m1.getHeader().getSequenceNumber() - m2.getHeader().getSequenceNumber();
			}
		};
		this.releasePartialSequences = releasePartialSequences;
		this.lastReleasedSequenceNumber = 0;
	}


	protected void addMessage(Message<?> message) {
		int insertionPoint = Collections.binarySearch(messages, message, resequencingComparator);
		if (insertionPoint < 0) {
			insertionPoint = -(insertionPoint + 1);
			this.messages.add(insertionPoint, message);
		}
	}

	protected boolean hasReceivedAllMessages() {
		//verify that there is a contiguous sequence of messages from the first to the last
		//and the last message is last in sequence, and the first message is the next one to be delivered
		//(aggregated, this means that the last possibile partial sequence of messages has been received
		Message<?> firstMessage = this.messages.get(0);
		Message<?> lastMessage = this.messages.get(messages.size() - 1);
		return (lastMessage.getHeader().getSequenceNumber() == lastMessage.getHeader().getSequenceSize()
				&& (lastMessage.getHeader().getSequenceNumber() - firstMessage.getHeader().getSequenceNumber()
				== this.messages.size() - 1
				&& this.lastReleasedSequenceNumber == firstMessage.getHeader().getSequenceNumber() - 1));
	}

	protected List<Message<?>> releaseAvailableMessages() {
		if (this.releasePartialSequences || hasReceivedAllMessages()) {
			ArrayList<Message<?>> releasedMessages = new ArrayList<Message<?>>();
			Iterator<Message<?>> it = this.messages.iterator();
			while (it.hasNext()) {
				Message<?> currentMessage = it.next();
				if (this.lastReleasedSequenceNumber == currentMessage.getHeader().getSequenceNumber() - 1) {
					releasedMessages.add(currentMessage);
					this.lastReleasedSequenceNumber = currentMessage.getHeader().getSequenceNumber();
					it.remove();
				}
				else {
					break;
				}
			}
			return releasedMessages;
		}
		else {
			return new ArrayList<Message<?>>();
		}
	}
	
}
