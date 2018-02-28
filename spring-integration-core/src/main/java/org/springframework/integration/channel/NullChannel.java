/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.AbstractMessageChannelMetrics;
import org.springframework.integration.support.management.ConfigurableMetricsAware;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.MessageChannelMetrics;
import org.springframework.integration.support.management.Statistics;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

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
public class NullChannel implements PollableChannel, MessageChannelMetrics,
		ConfigurableMetricsAware<AbstractMessageChannelMetrics>, BeanNameAware, NamedComponent {

	private final Log logger = LogFactory.getLog(getClass());

	private final ManagementOverrides managementOverrides = new ManagementOverrides();

	private volatile AbstractMessageChannelMetrics channelMetrics = new DefaultMessageChannelMetrics("nullChannel");

	private volatile boolean countsEnabled;

	private volatile boolean statsEnabled;

	private volatile boolean loggingEnabled = true;

	private String beanName;

	private MeterRegistry meterRegistry;

	private Timer successTimer;

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
		this.channelMetrics = new DefaultMessageChannelMetrics(getComponentName());
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
	public String getComponentName() {
		return StringUtils.hasText(this.beanName) ? this.beanName : "nullChannel";
	}

	@Override
	public String getComponentType() {
		return "channel";
	}

	@Override
	public void registerMeterRegistry(MeterRegistry registry) {
		this.meterRegistry = registry;
	}

	@Override
	public void configureMetrics(AbstractMessageChannelMetrics metrics) {
		Assert.notNull(metrics, "'metrics' must not be null");
		this.channelMetrics = metrics;
		this.managementOverrides.metricsConfigured = true;
	}

	@Override
	public void reset() {
		this.channelMetrics.reset();
	}

	@Override
	public void setCountsEnabled(boolean countsEnabled) {
		this.countsEnabled = countsEnabled;
		this.managementOverrides.countsConfigured = true;
		if (!countsEnabled) {
			this.statsEnabled = false;
			this.managementOverrides.statsConfigured = true;
		}
	}

	@Override
	public boolean isCountsEnabled() {
		return this.countsEnabled;
	}

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

	@Override
	public boolean isStatsEnabled() {
		return this.statsEnabled;
	}

	@Override
	public int getSendCount() {
		return this.channelMetrics.getSendCount();
	}

	@Override
	public long getSendCountLong() {
		return this.channelMetrics.getSendCountLong();
	}

	@Override
	public int getSendErrorCount() {
		return this.channelMetrics.getSendErrorCount();
	}

	@Override
	public long getSendErrorCountLong() {
		return this.channelMetrics.getSendErrorCountLong();
	}

	@Override
	public double getTimeSinceLastSend() {
		return this.channelMetrics.getTimeSinceLastSend();
	}

	@Override
	public double getMeanSendRate() {
		return this.channelMetrics.getMeanSendRate();
	}

	@Override
	public double getMeanErrorRate() {
		return this.channelMetrics.getMeanErrorRate();
	}

	@Override
	public double getMeanErrorRatio() {
		return this.channelMetrics.getMeanErrorRatio();
	}

	@Override
	public double getMeanSendDuration() {
		return this.channelMetrics.getMeanSendDuration();
	}

	@Override
	public double getMinSendDuration() {
		return this.channelMetrics.getMinSendDuration();
	}

	@Override
	public double getMaxSendDuration() {
		return this.channelMetrics.getMaxSendDuration();
	}

	@Override
	public double getStandardDeviationSendDuration() {
		return this.channelMetrics.getStandardDeviationSendDuration();
	}

	@Override
	public Statistics getSendDuration() {
		return this.channelMetrics.getSendDuration();
	}

	@Override
	public Statistics getSendRate() {
		return this.channelMetrics.getSendRate();
	}

	@Override
	public Statistics getErrorRate() {
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
			if (this.meterRegistry != null) {
				sendTimer().record(0, TimeUnit.MILLISECONDS);
			}
			this.channelMetrics.afterSend(this.channelMetrics.beforeSend(), true);
		}
		return true;
	}

	private Timer sendTimer() {
		if (this.successTimer == null) {
			this.successTimer = Timer.builder(SEND_TIMER_NAME)
					.tag("type", "channel")
					.tag("name", getComponentName() == null ? "unknown" : getComponentName())
					.tag("result", "success")
					.tag("exception", "none")
					.description("Subflow process time")
					.register(this.meterRegistry);
		}
		return this.successTimer;
	}

	@Override
	public Message<?> receive() {
		if (this.loggingEnabled && this.logger.isDebugEnabled()) {
			this.logger.debug("receive called on null channel");
		}
		return null;
	}

	@Override
	public Message<?> receive(long timeout) {
		return this.receive();
	}

	@Override
	public String toString() {
		return (this.beanName != null) ? this.beanName : super.toString();
	}

}
