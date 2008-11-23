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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
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
 * @author Marius Bogoevici
 */
public class Resequencer extends AbstractMessageBarrierHandler<SortedMap<Integer, Message<?>>, Integer> {

	private volatile boolean releasePartialSequences = true;


	public void setReleasePartialSequences(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	@Override
	protected MessageBarrier<SortedMap<Integer, Message<?>>, Integer> createMessageBarrier() {
		MessageBarrier<SortedMap<Integer, Message<?>>, Integer> messageBarrier 
			= new MessageBarrier<SortedMap<Integer, Message<?>>, Integer>(new TreeMap<Integer, Message<?>>());
		messageBarrier.getMessages().put(0, createFlagMessage(0));
		return messageBarrier;
	}
	
	@Override
	protected void processBarrier(MessageBarrier<SortedMap<Integer, Message<?>>, Integer> barrier) {
		if (hasReceivedAllMessages(barrier.getMessages())) {
			barrier.setComplete();
		}
		List<Message<?>> releasedMessages = releaseAvailableMessages(barrier);
		if (!CollectionUtils.isEmpty(releasedMessages)) {
			Message<?> lastMessage =  releasedMessages.get(releasedMessages.size()-1);
			if (lastMessage.getHeaders().getSequenceNumber().equals(lastMessage.getHeaders().getSequenceSize() - 1)) {
				this.removeBarrier(barrier.getCorrelationId());
			}
			this.sendReplies(releasedMessages, this.resolveReplyChannelFromMessage(releasedMessages.get(0)));
		}
	}

	private boolean hasReceivedAllMessages(SortedMap <Integer, Message<?>> messages) {
		Message<?> firstMessage = messages.get(messages.firstKey());
		Message<?> lastMessage = messages.get(messages.lastKey());
		return (lastMessage.getHeaders().getSequenceNumber().equals(lastMessage.getHeaders().getSequenceSize())
				&& (lastMessage.getHeaders().getSequenceNumber() - firstMessage.getHeaders().getSequenceNumber() == messages.size() - 1));
	}

	private List<Message<?>> releaseAvailableMessages(MessageBarrier<SortedMap<Integer, Message<?>>, Integer> barrier) {
		if (this.releasePartialSequences || barrier.isComplete()) {
			ArrayList<Message<?>> releasedMessages = new ArrayList<Message<?>>();
			Iterator<Message<?>> it = barrier.getMessages().values().iterator();
			//remove the initial flag from the list
			Message<?> flag = it.next();
			it.remove();
			int lastReleasedSequenceNumber = flag.getHeaders().getSequenceNumber();
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
			//re-insert the flag so that we know where to start releasing next
			barrier.getMessages().put(lastReleasedSequenceNumber, createFlagMessage(lastReleasedSequenceNumber));
			return releasedMessages;
		}
		else {
			return new ArrayList<Message<?>>();
		}
	}

	@Override
	protected boolean canAddMessage(Message<?> message,
			MessageBarrier<SortedMap<Integer, Message<?>>, Integer> barrier) {
		if (!super.canAddMessage(message, barrier)) {
			return false;
		}
		Message<?> flagMessage = barrier.getMessages().get(barrier.getMessages().firstKey());
		if (barrier.messages.containsKey(message.getHeaders().getSequenceNumber()) 
				|| flagMessage.getHeaders().getSequenceNumber() >= message.getHeaders().getSequenceNumber()) {
			logger.debug("A message with the same sequence number has been already received: " + message);
			return false;
		}
		Message<?> lastMessage = barrier.getMessages().get(barrier.getMessages().lastKey());
		if (lastMessage != flagMessage
				&& lastMessage.getHeaders().getSequenceSize() < message.getHeaders().getSequenceNumber()) {
			logger.debug("The message has a sequence number which is larger than the sequence size: "+ message);
			return false;
		}
		return true;
	}
	
	@Override
	protected void doAddMessage(Message<?> message, MessageBarrier<SortedMap<Integer, Message<?>>, Integer> barrier) {
		//add the message to the barrier, indexing it by its sequence number
		barrier.getMessages().put(message.getHeaders().getSequenceNumber(), message);
	}

	private static Message<Integer> createFlagMessage(int sequenceNumber) {
		return MessageBuilder.withPayload(sequenceNumber).setSequenceNumber(sequenceNumber).build();
	}

}
