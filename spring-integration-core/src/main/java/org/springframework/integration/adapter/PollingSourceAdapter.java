/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter;

import java.util.Collection;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.bus.MessageDispatcher;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.MessageMapper;
import org.springframework.util.Assert;

/**
 * A channel adapter that retrieves objects from a {@link PollableSource},
 * delegates to a {@link MessageMapper} to create messages from those objects,
 * and then sends the resulting messages to the provided {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class PollingSourceAdapter<T> extends AbstractSourceAdapter<T> implements MessageDispatcher {

	private static int DEFAULT_PERIOD = 1000;

	private PollableSource<T> source;

	private volatile boolean running;


	public PollingSourceAdapter(PollableSource<T> source) {
		Assert.notNull(source, "'source' must not be null");
		this.source = source;
		this.setConsumerPolicy(ConsumerPolicy.newPollingPolicy(DEFAULT_PERIOD));
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		if (!this.isInitialized()) {
			this.afterPropertiesSet();
		}
		this.running = true;
	}

	public void stop() {
		this.running = false;
	}

	public void setPeriod(int period) {
		Assert.isTrue(period > 0, "'period' must be a positive value");
		this.getConsumerPolicy().setPeriod(period);
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask > 0, "'maxMessagesPerTask' must be a positive value");
		this.getConsumerPolicy().setMaxMessagesPerTask(maxMessagesPerTask);
	}

	public int dispatch() {
		if (!this.isRunning()) {
			return 0;
		}
		int messagesProcessed = 0;
		int limit = this.getConsumerPolicy().getMaxMessagesPerTask();
		Collection<T> results = this.source.poll(limit);
		if (results != null) {
			if (results.size() > limit) {
				throw new MessageHandlingException("source returned too many results, the limit is " + limit);
			}
			for (T next : results) {
				if (this.sendToChannel(next)) {
					messagesProcessed++;
				}
			}
		}
		return messagesProcessed;
	}

}
