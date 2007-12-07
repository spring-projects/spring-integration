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

package org.springframework.integration.channel.consumer;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.MessageSource;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;

/**
 * Base class for consumers that poll on a given interval.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractPollingConsumer extends AbstractConsumer {

	private static final int DEFAULT_POLL_INTERVAL = 1000;


	protected ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	private int initialDelay = 0;

	private int pollInterval = DEFAULT_POLL_INTERVAL;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;


	public AbstractPollingConsumer(MessageSource source, MessageEndpoint endpoint) {
		super(source, endpoint);
		this.setReceiveTimeout(0);
	}


	public void setPollInterval(int pollInterval) {
		this.pollInterval = pollInterval;
	}

	/**
	 * Specify the {@link TimeUnit} for polling. Default is milliseconds.
	 */
	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	@Override
	protected void doStart() {
		scheduleInvoker(new PollingInvoker(), this.initialDelay, this.pollInterval, this.timeUnit);
	}

	/**
	 * Each subclass must implement this method depending on its scheduling
	 * behavior (e.g. fixed-rate versus fixed-delay).
	 * 
	 * @param invoker the invoker task to schedule
	 * @param initialDelay the time in milliseconds to wait before the first
	 * poll
	 * @param pollInterval the polling interval in milliseconds
	 */
	protected abstract void scheduleInvoker(Runnable invoker, int initialDelay, int pollInterval, TimeUnit timeUnit);


	@Override
	protected void doInitialize() {
	}

	@Override
	protected void doStop() {
		this.executor.shutdown();
	}

	@Override
	protected void messageReceived(Message message) {
	}


	private class PollingInvoker implements Runnable {

		public void run() {
			receiveAndPassToEndpoint();
		}

	}

}
