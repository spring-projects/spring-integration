/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.integration.core.Message;
import org.springframework.util.CollectionUtils;

/**
 * An {@link AbstractMessageBarrierHandler} that waits for a group of
 * {@link Message Messages} to arrive and re-sends them in order, sorted
 * by their <code>sequenceNumber</code>.
 * <p>
 * This handler can either release partial sequences of messages or can
 * wait for the whole sequence to arrive before re-sending them.
 * <p>
 * All considerations regarding <code>timeout</code> and grouping by
 * '<code>correlationId</code>' from {@link AbstractMessageBarrierHandler}
 * apply here as well.
 * 
 * It is assumed that all messages have the same <code>sequence_size</code> header attribute
 * and that the sequence numbers of the messages are successive, starting with
 * 1 up to <code>sequenceSize</code>. Messages that do not satisfy this condition are
 * considered out-of-sequence and thus rejected.
 * 
 *
 * Note: messages with the same sequence number will be treated as equivalent
 * by this class (i.e. after a message with a given sequence number is received,
 * further messages from within the same group, that have the same sequence number,
 * will be ignored.
 *
 * @author Marius Bogoevici
 * @author Alex Peters
 */
public class Resequencer extends AbstractMessageBarrierHandler<SortedSet<Message<?>>> {

	private volatile boolean releasePartialSequences = true;
	
	private static final String LAST_RELEASED_SEQUENCE_NUMBER = "last.released.sequence.number";


	public void setReleasePartialSequences(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	@Override
	public String getComponentType() {
    	return "resequencer";
	}

	@Override
	protected MessageBarrier<SortedSet<Message<?>>> createMessageBarrier(Object correlationKey) {
		MessageBarrier<SortedSet<Message<?>>> messageBarrier
			= new MessageBarrier<SortedSet<Message<?>>>(new TreeSet<Message<?>>(new MessageSequenceComparator()), correlationKey);
		messageBarrier.setAttribute(LAST_RELEASED_SEQUENCE_NUMBER, 0);
		return messageBarrier;
	}
	
	@Override
	protected void processBarrier(MessageBarrier<SortedSet<Message<?>>> barrier) {
		if (hasReceivedAllMessages(barrier)) {
			barrier.setComplete();
		}
		List<Message<?>> releasedMessages = releaseAvailableMessages(barrier);
		if (!CollectionUtils.isEmpty(releasedMessages)) {
			Message<?> lastMessage =  releasedMessages.get(releasedMessages.size()-1);
			if (lastMessage.getHeaders().getSequenceNumber().equals(lastMessage.getHeaders().getSequenceSize())) {
				this.removeBarrier(barrier.getCorrelationKey());
			}
			this.sendReplies(releasedMessages, this.resolveReplyChannelFromMessage(releasedMessages.get(0)));
		}
	}

	private boolean hasReceivedAllMessages(MessageBarrier<SortedSet<Message<?>>> barrier) {
		if(barrier.getMessages().isEmpty()) {
			return false;
		}
		int sequenceSize = barrier.getMessages().first().getHeaders().getSequenceSize();
		int messagesCurrentlyInBarrier = barrier.getMessages().size();
		Integer lastReleasedSequenceNumber = barrier.getAttribute(LAST_RELEASED_SEQUENCE_NUMBER);
		return (lastReleasedSequenceNumber + messagesCurrentlyInBarrier == sequenceSize);
	}

	private List<Message<?>> releaseAvailableMessages(MessageBarrier<SortedSet<Message<?>>> barrier) {
		if (this.releasePartialSequences || barrier.isComplete()) {
			ArrayList<Message<?>> releasedMessages = new ArrayList<Message<?>>();
			Iterator<Message<?>> it = barrier.getMessages().iterator();
			Integer lastReleasedSequenceNumber = barrier.getAttribute(LAST_RELEASED_SEQUENCE_NUMBER);
			while (it.hasNext()) {
				Message<?> currentMessage = it.next();
				if (lastReleasedSequenceNumber == currentMessage.getHeaders().getSequenceNumber() - 1) {
					releasedMessages.add(currentMessage);
					lastReleasedSequenceNumber = currentMessage.getHeaders().getSequenceNumber();
					it.remove();
				}
				else {
					break;
				}
			}
			barrier.setAttribute(LAST_RELEASED_SEQUENCE_NUMBER, lastReleasedSequenceNumber);
			return releasedMessages;
		}
		else {
			return new ArrayList<Message<?>>();
		}
	}

	@Override
	protected boolean canAddMessage(Message<?> message, MessageBarrier<SortedSet<Message<?>>> barrier) {
		if (!super.canAddMessage(message, barrier)) {
			return false;
		}
		Integer lastReleasedSequenceNumber = barrier.getAttribute(LAST_RELEASED_SEQUENCE_NUMBER);
		if (barrier.messages.contains(message)
				|| lastReleasedSequenceNumber >= message.getHeaders().getSequenceNumber()) {
			logger.debug("A message with the same sequence number has been already received: " + message);
			return false;
		}
		if (message.getHeaders().getSequenceSize() < message.getHeaders().getSequenceNumber()) {
			logger.debug("The message has a sequence number which is larger than the sequence size: "+ message);
			return false;
		}
		if (!barrier.getMessages().isEmpty() &&
				! message.getHeaders().getSequenceSize().equals(barrier.getMessages().first().getHeaders().getSequenceSize())) {
			logger.debug("The message has a sequence size which is different from other messages handled so far: " + message 
						+ ", expected value is " + barrier.getMessages().first().getHeaders().getSequenceNumber());
			return false;
		}
		return true;
	}

}
