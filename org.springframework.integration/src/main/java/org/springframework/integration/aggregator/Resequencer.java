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

import java.util.List;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;

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
public class Resequencer extends AbstractMessageBarrierHandler {

	private volatile boolean releasePartialSequences = true;


	public void setReleasePartialSequences(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	protected MessageBarrier createMessageBarrier() {
		return new ResequencingMessageBarrier(this.releasePartialSequences);
	}

	protected Message<?>[] processReleasedMessages(Object correlationId, List<Message<?>> messages) {
		return messages.toArray(new Message<?>[messages.size()]);
	}

	protected boolean isBarrierRemovable(Object correlationId, List<Message<?>> releasedMessages) {
		MessageHeaders lastMessageHeaders = releasedMessages.get(releasedMessages.size() - 1).getHeaders(); 
		return (lastMessageHeaders.getSequenceNumber() == lastMessageHeaders.getSequenceSize());
	}

}
