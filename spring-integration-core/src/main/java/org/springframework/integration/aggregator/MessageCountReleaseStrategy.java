/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.integration.store.MessageGroup;

/**
 * A {@link ReleaseStrategy} that releases only the first <code>n</code> messages, where <code>n</code> is a threshold.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 *
 */
public class MessageCountReleaseStrategy implements ReleaseStrategy {

	private final int threshold;

	/**
	 * @param threshold the number of messages to accept before releasing
	 */
	public MessageCountReleaseStrategy(int threshold) {
		super();
		this.threshold = threshold;
	}

	/**
	 * Convenient constructor is only one message is required (threshold=1).
	 */
	public MessageCountReleaseStrategy() {
		this(1);
	}

	/**
	 * Release the group if it has more messages than the threshold and has not previously been released.
	 * It is possible that more messages than the threshold could be released, but only if multiple consumers
	 * receive messages from the same group concurrently.
	 */
	@Override
	public boolean canRelease(MessageGroup group) {
		return group.size() >= this.threshold;
	}

}
