/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;

/**
 * An implementation of {@link ReleaseStrategy} that simply compares the current size of
 * the message list to the expected 'sequenceSize'. Supports release of partial sequences.
 * Correlating message handlers prevent the addition of duplicate sequences to the group.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Enrique Rodriguez
 */
public class SequenceSizeReleaseStrategy implements ReleaseStrategy {

	private static final Log LOGGER = LogFactory.getLog(SequenceSizeReleaseStrategy.class);

	private final Comparator<Message<?>> comparator = new MessageSequenceComparator();

	private volatile boolean releasePartialSequences;

	/**
	 * Construct an instance that does not support releasing partial sequences.
	 */
	public SequenceSizeReleaseStrategy() {
		this(false);
	}

	/**
	 * Construct an instance that supports releasing partial sequences if
	 * releasePartialSequences is true. This can be an expensive operation on large
	 * groups.
	 * @param releasePartialSequences true to allow the release of partial sequences.
	 */
	public SequenceSizeReleaseStrategy(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	/**
	 * Flag that determines if partial sequences are allowed. If true then as soon as
	 * enough messages arrive that can be ordered they will be released, provided they
	 * all have sequence numbers greater than those already released.
	 * This can be an expensive operation for large groups.
	 * @param releasePartialSequences true when partial sequences should be released.
	 */
	public void setReleasePartialSequences(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	@Override
	public boolean canRelease(MessageGroup messageGroup) {
		boolean canRelease = false;
		int size = messageGroup.size();
		if (this.releasePartialSequences && size > 0) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Considering partial release of group [" + messageGroup + "]");
			}
			Collection<Message<?>> messages = messageGroup.getMessages();
			Message<?> minMessage = Collections.min(messages, this.comparator);

			int nextSequenceNumber = StaticMessageHeaderAccessor.getSequenceNumber(minMessage);
			int lastReleasedMessageSequence = messageGroup.getLastReleasedMessageSequenceNumber();

			if (nextSequenceNumber - lastReleasedMessageSequence == 1) {
				canRelease = true;
			}
		}
		else {
			if (size == 0) {
				canRelease = true;
			}
			else {
				int sequenceSize = messageGroup.getSequenceSize();
				// If there is no sequence then it must be incomplete....
				if (sequenceSize == size) {
					canRelease = true;
				}
			}
		}
		return canRelease;
	}

}
