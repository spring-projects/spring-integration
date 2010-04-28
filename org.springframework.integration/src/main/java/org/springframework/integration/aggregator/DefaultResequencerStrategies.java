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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;

/**
 * This class implements all the strategy interfaces needed for a default
 * resequencer.
 * 
 * @author Iwein Fuld
 * @since 2.0
 */
public class DefaultResequencerStrategies implements CorrelationStrategy, CompletionStrategy, MessageGroupProcessor {

	private final ConcurrentMap<Object, AtomicInteger> nextMessagesToPass = new ConcurrentHashMap<Object, AtomicInteger>();
	private final ConcurrentMap<Object, AtomicInteger> lastMessagesToPass = new ConcurrentHashMap<Object, AtomicInteger>();

	private volatile SequenceNumberComparator sequenceNumberComparator = new SequenceNumberComparator();

	private volatile boolean releasePartialSequences;

	public Object getCorrelationKey(Message<?> message) {
		Object key = message.getHeaders().getCorrelationId();
		nextMessagesToPass.putIfAbsent(key, new AtomicInteger(1));
		return key;
	}

	public boolean isComplete(Collection<? extends Message<?>> messages) {
		return releasePartialSequences
				|| (!messages.isEmpty() && messages.iterator().next().getHeaders().getSequenceSize() == messages.size());
	}

	public void processAndSend(MessageGroup group, MessageChannelTemplate channelTemplate, MessageChannel outputChannel) {
		Collection<Message<?>> all = group.getMessages();
		Object correlationKey = group.getCorrelationKey();
		if (all.size() > 0) {
			List<Message<?>> sorted = new ArrayList<Message<?>>(all);
			Collections.sort(sorted, sequenceNumberComparator);
			AtomicInteger nextSequence = nextMessagesToPass.get(correlationKey);
			for (Message<?> message : sorted) {
				final int sequenceNumber = message.getHeaders().getSequenceNumber();
				if (sequenceNumber <= nextSequence.get()) {
					channelTemplate.send(message, outputChannel);
					nextSequence.compareAndSet(sequenceNumber, sequenceNumber + 1);
				}
			}
			MessageHeaders headers = sorted.get(0).getHeaders();
			if (all.size() == headers.getSequenceSize()) {
				// TODO: it's only complete if this is true...
			}
		}
	}

	public void setReleasePartialSequences(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	private static class SequenceNumberComparator implements Comparator<Message<?>> {
		public int compare(Message<?> o1, Message<?> o2) {
			return o1.getHeaders().getSequenceNumber().compareTo(o2.getHeaders().getSequenceNumber());
		}
	}

}
