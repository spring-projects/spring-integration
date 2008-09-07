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

package org.springframework.integration.endpoint;

import org.springframework.integration.dispatcher.SimpleDispatcher;
import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.message.SubscribableSource;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class SourcePoller extends AbstractPoller implements SubscribableSource {

	private final PollableSource<?> source;

	private final SimpleDispatcher dispatcher = new SimpleDispatcher();

	private volatile long receiveTimeout = 1000;


	public SourcePoller(PollableSource<?> source, Schedule schedule) {
		super(schedule);
		Assert.notNull(source, "source must not be null");
		this.source = source;
	}


	/**
	 * Specify the timeout to use when receiving from the source (in milliseconds).
	 * This value will only apply if the source is a {@link BlockingSource}.
	 * <p>
	 * A negative value indicates that receive calls should block indefinitely.
	 * The default value is 1000 (1 second).
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public boolean subscribe(MessageEndpoint endpoint) {
		return this.dispatcher.subscribe(endpoint);
	}

	public boolean unsubscribe(MessageEndpoint endpoint) {
		return this.dispatcher.unsubscribe(endpoint);
	}

	@Override
	protected boolean doPoll() {
		Message<?> message = (this.receiveTimeout >= 0 && this.source instanceof BlockingSource)
				? ((BlockingSource<?>) this.source).receive(this.receiveTimeout)
				: this.source.receive();
		if (message == null) {
			return false;
		}
		return this.dispatcher.dispatch(message);
	}

}
