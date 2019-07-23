/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;

/**
 * A {@link ReleaseStrategy} that releases all messages if any of the following is true:
 *
 * <ul>
 * <li>The sequence is complete (if there is one).</li>
 * <li>There are more messages than a threshold set by the user.</li>
 * <li>The time elapsed since the earliest message, according to their timestamps, if
 * present, exceeds a timeout set by the user.</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Gary Russell
 * @author Peter Uhlenbruck
 *
 * @since 2.0
 */
public class TimeoutCountSequenceSizeReleaseStrategy implements ReleaseStrategy {

	/**
	 * Default timeout is one minute.
	 */
	public static final long DEFAULT_TIMEOUT = 60 * 1000;

	/**
	 * Default threshold is effectively infinite.
	 */
	public static final int DEFAULT_THRESHOLD = Integer.MAX_VALUE;

	private final int threshold;

	private final long timeout;

	public TimeoutCountSequenceSizeReleaseStrategy() {
		this(DEFAULT_THRESHOLD, DEFAULT_TIMEOUT);
	}

	/**
	 * @param threshold the number of messages to accept before releasing
	 * @param timeout the timeout for the release in milliseconds
	 */
	public TimeoutCountSequenceSizeReleaseStrategy(int threshold, long timeout) {
		this.threshold = threshold;
		this.timeout = timeout;
	}

	@Override
	public boolean canRelease(MessageGroup messages) {
		long elapsedTime = System.currentTimeMillis() - findEarliestTimestamp(messages);
		return messages.isComplete() || messages.getMessages().size() >= this.threshold || elapsedTime > this.timeout;
	}

	/**
	 * @param messages the message group
	 * @return the earliest timestamp or Long.MAX_VALUE
	 */
	private long findEarliestTimestamp(MessageGroup messages) {
		long result = Long.MAX_VALUE;
		for (Message<?> message : messages.getMessages()) {
			Long timestamp = message.getHeaders().getTimestamp();
			if (timestamp != null && timestamp < result) {
				result = timestamp;
			}
		}
		return result;
	}

}
