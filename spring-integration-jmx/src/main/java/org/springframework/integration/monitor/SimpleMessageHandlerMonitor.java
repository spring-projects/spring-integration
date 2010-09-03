/**
 * 
 */
package org.springframework.integration.monitor;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.jmx.export.annotation.ManagedMetric;
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
public class SimpleMessageHandlerMonitor implements MessageHandler, MessageHandlerMonitor {

	private static final Log logger = LogFactory.getLog(SimpleMessageHandlerMonitor.class);

	private static final int DEFAULT_MOVING_AVERAGE_WINDOW = 10;

	private final MessageHandler handler;

	private final AtomicInteger handleCount = new AtomicInteger();

	private final AtomicInteger errorCount = new AtomicInteger();

	private final ExponentialMovingAverageCumulativeHistory duration = new ExponentialMovingAverageCumulativeHistory(
			DEFAULT_MOVING_AVERAGE_WINDOW);

	private String name;

	private String source;

	public SimpleMessageHandlerMonitor(MessageHandler handler) {
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

	public void handleMessage(Message<?> message) throws MessageRejectedException, MessageHandlingException,
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

			handler.handleMessage(message);

			timer.stop();
			duration.append(timer.getTotalTimeSeconds());
		} catch (RuntimeException e) {
			errorCount.incrementAndGet();
			throw e;
		} catch (Error e) {
			errorCount.incrementAndGet();
			throw e;
		}
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

	@Override
	public String toString() {
		return String.format("MessageHandlerMonitor: [name=%s, source=%s, duration=%s]", name, source, duration);
	}

}