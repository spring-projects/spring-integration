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
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;
import org.springframework.util.StopWatch;

/**
 * @author Dave Syer
 * 
 * @since 2.0
 *
 */
@ManagedResource
public class SimpleMessageHandlerMetrics implements MethodInterceptor, MessageHandlerMetrics {

	private static final Log logger = LogFactory.getLog(SimpleMessageHandlerMetrics.class);

	private static final int DEFAULT_MOVING_AVERAGE_WINDOW = 10;

	private final MessageHandler handler;

	private final AtomicInteger activeCount = new AtomicInteger();

	private final AtomicInteger handleCount = new AtomicInteger();

	private final AtomicInteger errorCount = new AtomicInteger();

	private final ExponentialMovingAverage duration = new ExponentialMovingAverage(
			DEFAULT_MOVING_AVERAGE_WINDOW);

	private String name;

	private String source;

	public SimpleMessageHandlerMetrics(MessageHandler handler) {
		this.handler = handler;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return this.source;
	}

	public MessageHandler getMessageHandler() {
		return handler;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		String method = invocation.getMethod().getName();
		if ("handleMessage".equals(method)) {
			Message<?> message = (Message<?>) invocation.getArguments()[0];
			handleMessage(message);
			return null;
		}
		return invocation.proceed();
	}

	private void handleMessage(Message<?> message) throws MessageRejectedException, MessageHandlingException,
			MessageDeliveryException {
		if (logger.isTraceEnabled()) {
			logger.trace("messageHandler(" + handler + ") message(" + message + ") :");
		}

		String name = this.name;
		if (name == null) {
			name = handler.toString();
		}
		StopWatch timer = new StopWatch(name + ".handle:execution");

		try {
			timer.start();
			handleCount.incrementAndGet();
			activeCount.incrementAndGet();

			handler.handleMessage(message);

			timer.stop();
			duration.append(timer.getTotalTimeSeconds());
		} catch (RuntimeException e) {
			errorCount.incrementAndGet();
			throw e;
		} catch (Error e) {
			errorCount.incrementAndGet();
			throw e;
		} finally {
			activeCount.decrementAndGet();
		}
	}

	@ManagedOperation
	public synchronized void reset() {
		duration.reset();
		errorCount.set(0);
		handleCount.set(0);
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Handler Execution Count", description = "rate=1h")
	public int getHandleCount() {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting Handle Count:" + this);
		}
		return handleCount.get();
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Handler Error Count", description = "rate=1h")
	public int getErrorCount() {
		return errorCount.get();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Handler Mean Duration")
	public double getMeanDuration() {
		return duration.getMean();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Handler Min Duration")
	public double getMinDuration() {
		return duration.getMin();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Handler Max Duration")
	public double getMaxDuration() {
		return duration.getMax();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Handler Standard Deviation Duration")
	public double getStandardDeviationDuration() {
		return duration.getStandardDeviation();
	}
	
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Handler Active Count")
	public int getActiveCount() {
		return activeCount.get();
	}
	
	public Statistics getDuration() {
		return duration.getStatistics();
	}

	@Override
	public String toString() {
		return String.format("MessageHandlerMonitor: [name=%s, source=%s, duration=%s]", name, source, duration);
	}

}