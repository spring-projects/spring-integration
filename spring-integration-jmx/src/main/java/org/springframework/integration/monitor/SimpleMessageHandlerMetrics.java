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

package org.springframework.integration.monitor;

import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
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

	private final AtomicInteger activeCount = new AtomicInteger();

	private final AtomicInteger handleCount = new AtomicInteger();

	private final AtomicInteger errorCount = new AtomicInteger();

	private final ExponentialMovingAverage duration = new ExponentialMovingAverage(DEFAULT_MOVING_AVERAGE_WINDOW);

	private volatile String name;

	private volatile String source;


	public SimpleMessageHandlerMetrics(MessageHandler handler) {
		this.handler = handler;
	}


	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return this.source;
	}

	public MessageHandler getMessageHandler() {
		return this.handler;
	}

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

	public synchronized void reset() {
		this.duration.reset();
		this.errorCount.set(0);
		this.handleCount.set(0);
	}

	public int getHandleCount() {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting Handle Count:" + this);
		}
		return this.handleCount.get();
	}

	public int getErrorCount() {
		return this.errorCount.get();
	}

	public double getMeanDuration() {
		return this.duration.getMean();
	}

	public double getMinDuration() {
		return this.duration.getMin();
	}

	public double getMaxDuration() {
		return this.duration.getMax();
	}

	public double getStandardDeviationDuration() {
		return this.duration.getStandardDeviation();
	}

	public int getActiveCount() {
		return this.activeCount.get();
	}

	public Statistics getDuration() {
		return this.duration.getStatistics();
	}

	@Override
	public String toString() {
		return String.format("MessageHandlerMonitor: [name=%s, source=%s, duration=%s]", name, source, duration);
	}

}
