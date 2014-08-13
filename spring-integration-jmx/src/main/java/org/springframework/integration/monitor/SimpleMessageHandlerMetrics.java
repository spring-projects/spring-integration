/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.monitor;

import java.util.concurrent.atomic.AtomicLong;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.StopWatch;

/**
 * @author Dave Syer
 * @since 2.0
 */
@ManagedResource
public class SimpleMessageHandlerMetrics implements MethodInterceptor, MessageHandlerMetrics {

	private static final Log logger = LogFactory.getLog(SimpleMessageHandlerMetrics.class);

	private static final int DEFAULT_MOVING_AVERAGE_WINDOW = 10;


	private final MessageHandler handler;

	private final AtomicLong activeCount = new AtomicLong();

	private final AtomicLong handleCount = new AtomicLong();

	private final AtomicLong errorCount = new AtomicLong();

	private final ExponentialMovingAverage duration = new ExponentialMovingAverage(DEFAULT_MOVING_AVERAGE_WINDOW);

	private volatile String name;

	private volatile String source;


	public SimpleMessageHandlerMetrics(MessageHandler handler) {
		this.handler = handler;
	}


	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public String getSource() {
		return this.source;
	}

	public MessageHandler getMessageHandler() {
		return this.handler;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		String method = invocation.getMethod().getName();
		if ("handleMessage".equals(method)) {
			Message<?> message = (Message<?>) invocation.getArguments()[0];
			handleMessage(invocation, message);
			return null;
		}
		return invocation.proceed();
	}

	private void handleMessage(MethodInvocation invocation, Message<?> message) throws Throwable {
		if (logger.isTraceEnabled()) {
			logger.trace("messageHandler(" + this.handler + ") message(" + message + ") :");
		}
		String name = this.name;
		if (name == null) {
			name = this.handler.toString();
		}
		StopWatch timer = new StopWatch(name + ".handle:execution");
		try {
			timer.start();
			this.handleCount.incrementAndGet();
			this.activeCount.incrementAndGet();

			invocation.proceed();

			timer.stop();
			this.duration.append(timer.getTotalTimeMillis());
		}
		catch (Throwable e) {
			this.errorCount.incrementAndGet();
			throw e;
		}
		finally {
			this.activeCount.decrementAndGet();
		}
	}

	@Override
	public synchronized void reset() {
		this.duration.reset();
		this.errorCount.set(0);
		this.handleCount.set(0);
	}

	@Override
	public long getHandleCountLong() {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting Handle Count:" + this);
		}
		return this.handleCount.get();
	}

	@Override
	public int getHandleCount() {
		return (int) getHandleCountLong();
	}

	@Override
	public int getErrorCount() {
		return (int) this.errorCount.get();
	}

	@Override
	public long getErrorCountLong() {
		return this.errorCount.get();
	}

	@Override
	public double getMeanDuration() {
		return this.duration.getMean();
	}

	@Override
	public double getMinDuration() {
		return this.duration.getMin();
	}

	@Override
	public double getMaxDuration() {
		return this.duration.getMax();
	}

	@Override
	public double getStandardDeviationDuration() {
		return this.duration.getStandardDeviation();
	}

	@Override
	public int getActiveCount() {
		return (int) this.activeCount.get();
	}

	@Override
	public long getActiveCountLong() {
		return this.activeCount.get();
	}

	@Override
	public Statistics getDuration() {
		return this.duration.getStatistics();
	}

	@Override
	public String toString() {
		return String.format("MessageHandlerMonitor: [name=%s, source=%s, duration=%s]", name, source, duration);
	}

}
