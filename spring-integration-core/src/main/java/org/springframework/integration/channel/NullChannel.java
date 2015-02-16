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

package org.springframework.integration.channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.management.ChannelSendMetrics;
import org.springframework.integration.channel.management.MessageChannelMetrics;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.Statistics;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

/**
 * A channel implementation that essentially behaves like "/dev/null".
 * All receive() calls will return <em>null</em>, and all send() calls
 * will return <em>true</em> although no action is performed.
 * Note however that the invocations are logged at debug-level.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
@ManagedResource
public class NullChannel implements PollableChannel, MessageChannelMetrics, NamedComponent {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final ChannelSendMetrics metrics = new ChannelSendMetrics("nullChannel");

	private volatile boolean statsEnabled;

	@Override
	public String getComponentName() {
		return "nullChannel";
	}

	@Override
	public String getComponentType() {
		return "channel";
	}

	@Override
	public void reset() {
		metrics.reset();
	}

	@Override
	public void enableStats(boolean statsEnabled) {
		this.statsEnabled = statsEnabled;
	}

	@Override
	public boolean isStatsEnabled() {
		return this.statsEnabled;
	}

	@Override
	public int getSendCount() {
		return metrics.getSendCount();
	}

	@Override
	public long getSendCountLong() {
		return metrics.getSendCountLong();
	}

	@Override
	public int getSendErrorCount() {
		return metrics.getSendErrorCount();
	}

	@Override
	public long getSendErrorCountLong() {
		return metrics.getSendErrorCountLong();
	}

	@Override
	public double getTimeSinceLastSend() {
		return metrics.getTimeSinceLastSend();
	}

	@Override
	public double getMeanSendRate() {
		return metrics.getMeanSendRate();
	}

	@Override
	public double getMeanErrorRate() {
		return metrics.getMeanErrorRate();
	}

	@Override
	public double getMeanErrorRatio() {
		return metrics.getMeanErrorRatio();
	}

	@Override
	public double getMeanSendDuration() {
		return metrics.getMeanSendDuration();
	}

	@Override
	public double getMinSendDuration() {
		return metrics.getMinSendDuration();
	}

	@Override
	public double getMaxSendDuration() {
		return metrics.getMaxSendDuration();
	}

	@Override
	public double getStandardDeviationSendDuration() {
		return metrics.getStandardDeviationSendDuration();
	}

	@Override
	public Statistics getSendDuration() {
		return metrics.getSendDuration();
	}

	@Override
	public Statistics getSendRate() {
		return metrics.getSendRate();
	}

	@Override
	public Statistics getErrorRate() {
		return metrics.getErrorRate();
	}

	@Override
	public boolean send(Message<?> message) {
		if (logger.isDebugEnabled()) {
			logger.debug("message sent to null channel: " + message);
		}
		if (this.statsEnabled) {
			this.metrics.afterSend(this.metrics.beforeSend(), true);
		}
		return true;
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		return this.send(message);
	}

	@Override
	public Message<?> receive() {
		if (logger.isDebugEnabled()) {
			logger.debug("receive called on null channel");
		}
		return null;
	}

	@Override
	public Message<?> receive(long timeout) {
		return this.receive();
	}

}
