/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.metrics.CounterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.support.management.metrics.TimerFacade;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;

/**
 * A channel implementation that essentially behaves like "/dev/null".
 * All receive() calls will return <em>null</em>, and all send() calls
 * will return <em>true</em> although no action is performed.
 * Note however that the invocations are logged at debug-level.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artyem Bilan
 */
@IntegrationManagedResource
@SuppressWarnings("deprecation")
public class NullChannel implements PollableChannel,
		org.springframework.integration.support.management.MessageChannelMetrics,
		org.springframework.integration.support.management.ConfigurableMetricsAware<
				org.springframework.integration.support.management.AbstractMessageChannelMetrics>,
		BeanNameAware, NamedComponent, IntegrationPattern {

	private final Log logger = LogFactory.getLog(getClass());

	private final ManagementOverrides managementOverrides = new ManagementOverrides();

	private org.springframework.integration.support.management.AbstractMessageChannelMetrics channelMetrics
			= new org.springframework.integration.support.management.DefaultMessageChannelMetrics("nullChannel");

	private boolean countsEnabled;

	private boolean statsEnabled;

	private boolean loggingEnabled = true;

	private String beanName;

	private MetricsCaptor metricsCaptor;

	private TimerFacade successTimer;

	private CounterFacade receiveCounter;

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
		this.channelMetrics =
				new org.springframework.integration.support.management.DefaultMessageChannelMetrics(this.beanName);
	}

	@Override
	public boolean isLoggingEnabled() {
		return this.loggingEnabled;
	}

	@Override
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
		this.managementOverrides.loggingConfigured = true;
	}

	@Override
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	@Nullable
	public String getComponentName() {
		return this.beanName;
	}

	@Override
	public String getComponentType() {
		return "null-channel";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.null_channel;
	}

	@Override
	public void registerMetricsCaptor(MetricsCaptor registry) {
		this.metricsCaptor = registry;
	}

	@Override
	public void configureMetrics(
			org.springframework.integration.support.management.AbstractMessageChannelMetrics metrics) {

		Assert.notNull(metrics, "'metrics' must not be null");
		this.channelMetrics = metrics;
		this.managementOverrides.metricsConfigured = true;
	}

	/**
	 * Deprecated.
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public void reset() {
		this.channelMetrics.reset();
	}

	/**
	 * Deprecated.
	 * @param countsEnabled the countsEnabled
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public void setCountsEnabled(boolean countsEnabled) {
		this.countsEnabled = countsEnabled;
		this.managementOverrides.countsConfigured = true;
		if (!countsEnabled) {
			this.statsEnabled = false;
			this.managementOverrides.statsConfigured = true;
		}
	}

	/**
	 * Deprecated.
	 * @return counts enabled
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public boolean isCountsEnabled() {
		return this.countsEnabled;
	}

	/**
	 * Deprecated.
	 * @param statsEnabled the statsEnabled
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public void setStatsEnabled(boolean statsEnabled) {
		if (statsEnabled) {
			this.countsEnabled = true;
			this.managementOverrides.countsConfigured = true;
		}
		this.statsEnabled = statsEnabled;
		this.channelMetrics.setFullStatsEnabled(statsEnabled);
		this.managementOverrides.statsConfigured = true;
	}

	/**
	 * Deprecated.
	 * @return stats enabled
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public boolean isStatsEnabled() {
		return this.statsEnabled;
	}

	/**
	 * Deprecated.
	 * @return send count
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public int getSendCount() {
		return this.channelMetrics.getSendCount();
	}

	/**
	 * Deprecated.
	 * @return send count
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public long getSendCountLong() {
		return this.channelMetrics.getSendCountLong();
	}

	/**
	 * Deprecated.
	 * @return error count
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public int getSendErrorCount() {
		return this.channelMetrics.getSendErrorCount();
	}

	/**
	 * Deprecated.
	 * @return error count
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public long getSendErrorCountLong() {
		return this.channelMetrics.getSendErrorCountLong();
	}

	/**
	 * Deprecated.
	 * @return time since last send
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public double getTimeSinceLastSend() {
		return this.channelMetrics.getTimeSinceLastSend();
	}

	/**
	 * Deprecated.
	 * @return mean send rate
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public double getMeanSendRate() {
		return this.channelMetrics.getMeanSendRate();
	}

	/**
	 * Deprecated.
	 * @return mean error rate
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public double getMeanErrorRate() {
		return this.channelMetrics.getMeanErrorRate();
	}

	/**
	 * Deprecated.
	 * @return mean error ratio
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public double getMeanErrorRatio() {
		return this.channelMetrics.getMeanErrorRatio();
	}

	/**
	 * Deprecated.
	 * @return mean send duration
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public double getMeanSendDuration() {
		return this.channelMetrics.getMeanSendDuration();
	}

	/**
	 * Deprecated.
	 * @return min send duration
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public double getMinSendDuration() {
		return this.channelMetrics.getMinSendDuration();
	}

	/**
	 * Deprecated.
	 * @return max send duration
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public double getMaxSendDuration() {
		return this.channelMetrics.getMaxSendDuration();
	}

	/**
	 * Deprecated.
	 * @return standard deviation send duration
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public double getStandardDeviationSendDuration() {
		return this.channelMetrics.getStandardDeviationSendDuration();
	}


	/**
	 * Deprecated.
	 * @return statistics
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public org.springframework.integration.support.management.Statistics getSendDuration() {
		return this.channelMetrics.getSendDuration();
	}

	/**
	 * Deprecated.
	 * @return statistics
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public org.springframework.integration.support.management.Statistics getSendRate() {
		return this.channelMetrics.getSendRate();
	}

	/**
	 * Deprecated.
	 * @return statistics
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@Override
	public org.springframework.integration.support.management.Statistics getErrorRate() {
		return this.channelMetrics.getErrorRate();
	}

	@Override
	public ManagementOverrides getOverrides() {
		return this.managementOverrides;
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		return send(message);
	}

	@Override
	public boolean send(Message<?> message) {
		if (this.loggingEnabled && this.logger.isDebugEnabled()) {
			this.logger.debug("message sent to null channel: " + message);
		}
		if (this.countsEnabled) {
			if (this.metricsCaptor != null) {
				sendTimer().record(0, TimeUnit.MILLISECONDS);
			}
			this.channelMetrics.afterSend(this.channelMetrics.beforeSend(), true);
		}
		return true;
	}

	private TimerFacade sendTimer() {
		if (this.successTimer == null) {
			this.successTimer =
					this.metricsCaptor.timerBuilder(SEND_TIMER_NAME)
							.tag("type", "channel")
							.tag("name", getComponentName() == null ? "nullChannel" : getComponentName())
							.tag("result", "success")
							.tag("exception", "none")
							.description("Subflow process time")
							.build();
		}
		return this.successTimer;
	}

	@Override
	public Message<?> receive() {
		if (this.loggingEnabled) {
			this.logger.debug("receive called on null channel");
		}
		incrementReceiveCounter();
		return null;
	}

	@Override
	public Message<?> receive(long timeout) {
		return receive();
	}

	private void incrementReceiveCounter() {
		if (this.metricsCaptor != null) {
			if (this.receiveCounter == null) {
				this.receiveCounter = buildReceiveCounter();
			}
			this.receiveCounter.increment();
		}
	}

	private CounterFacade buildReceiveCounter() {
		return this.metricsCaptor
				.counterBuilder(RECEIVE_COUNTER_NAME)
				.tag("name", getComponentName() == null ? "unknown" : getComponentName())
				.tag("type", "channel")
				.tag("result", "success")
				.tag("exception", "none")
				.description("Messages received")
				.build();
	}

	@Override
	public String toString() {
		return (this.beanName != null) ? this.beanName : super.toString();
	}

	@Override
	public void destroy() {
		if (this.successTimer != null) {
			this.successTimer.remove();
		}
		if (this.receiveCounter != null) {
			this.receiveCounter.remove();
		}
	}

}
