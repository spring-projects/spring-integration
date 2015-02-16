/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.channel.management;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
public class ChannelReceiveMetrics extends ChannelSendMetrics {

	private final AtomicLong receiveCount = new AtomicLong();

	private final AtomicLong receiveErrorCount = new AtomicLong();


	public ChannelReceiveMetrics(String name) {
		super(name);
	}

	public void afterReceive() {
		if (logger.isTraceEnabled()) {
			logger.trace("Recording receive on channel(" + getName() + ") ");
		}
		this.receiveCount.incrementAndGet();
	}

	public void afterError() {
		this.receiveErrorCount.incrementAndGet();
	}

	@Override
	@ManagedOperation
	public synchronized void reset() {
		super.reset();
		this.receiveErrorCount.set(0);
		this.receiveCount.set(0);
	}

	public int getReceiveCount() {
		return (int) this.receiveCount.get();
	}

	public long getReceiveCountLong() {
		return this.receiveCount.get();
	}

	public int getReceiveErrorCount() {
		return (int) this.receiveErrorCount.get();
	}

	public long getReceiveErrorCountLong() {
		return this.receiveErrorCount.get();
	}

	@Override
	public String toString() {
		return String.format("MessageChannelMonitor: [name=%s, sends=%d, receives=%d]",
				getName(), getSendCount(), this.receiveCount.get());
	}

}
