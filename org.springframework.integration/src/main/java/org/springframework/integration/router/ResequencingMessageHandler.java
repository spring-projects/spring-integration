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

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.integration.message.Message;


/**
 * An {@link AbstractMessageBarrierHandler} that waits for a group of
 * {@link Message Messages} to arrive and re-sends them in order, sorted
 * by their <code>sequenceNumber</code>.
 * <p>
 * This handler can either release partial sequences of messages or can
 * wait for the whole sequence to arrive before re-sending them.
 * <p>
 * All considerations regarding <code>timeout</code> and grouping by
 * '<code>correlationId</code>' from {@link AbstractMessageBarrierHandler} apply
 * here as well.
 *
 * @author Marius Bogoevici
 */
public class ResequencingMessageHandler extends AbstractMessageBarrierHandler{

	private boolean releasePartialSequences;

	public ResequencingMessageHandler(boolean releasePartialSequences) {
		this(null, releasePartialSequences);
	}

	public ResequencingMessageHandler(ScheduledExecutorService executor, boolean releasePartialSequences) {
		super(executor);
		this.releasePartialSequences = releasePartialSequences;
	}

	protected MessageBarrier createMessageBarrier() {
		return new ResequencingMessageBarrier(releasePartialSequences);
	}

	protected Message<?>[] processReleasedMessages(Object correlationId, List<Message<?>> messages) {
		return messages.toArray(new Message<?>[messages.size()]);
	}

	protected boolean isBarrierRemovable(Object correlationId, List<Message<?>> releasedMessages) {
		return (releasedMessages.get(releasedMessages.size() - 1).getHeader().getSequenceNumber() ==
				releasedMessages.get(releasedMessages.size() - 1).getHeader().getSequenceSize());
	}
}
