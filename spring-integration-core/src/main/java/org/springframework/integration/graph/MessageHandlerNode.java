/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHandler;

/**
 * Represents a message handler.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
@SuppressWarnings("deprecation")
public class MessageHandlerNode extends EndpointNode implements SendTimersAware {

	private final String input;

	private Supplier<SendTimers> sendTimers;

	public MessageHandlerNode(int nodeId, String name, MessageHandler handler, String input, String output) {
		super(nodeId, name, handler, output,
				handler instanceof org.springframework.integration.support.management.MessageHandlerMetrics
						? new Stats((org.springframework.integration.support.management.MessageHandlerMetrics) handler)
						: new IntegrationNode.Stats());
		this.input = input;
	}

	public String getInput() {
		return this.input;
	}

	@Nullable
	public SendTimers getSendTimers() {
		return this.sendTimers != null ? this.sendTimers.get() : null;
	}

	@Override
	public void sendTimers(Supplier<SendTimers> timers) {
		this.sendTimers = timers;
	}

	public static final class Stats extends IntegrationNode.Stats {

		private final org.springframework.integration.support.management.MessageHandlerMetrics handler;

		Stats(org.springframework.integration.support.management.MessageHandlerMetrics handler) {
			this.handler = handler;
		}

		@Override
		protected boolean isAvailable() {
			return this.handler.isCountsEnabled();
		}

		public boolean isLoggingEnabled() {
			return this.handler.isLoggingEnabled();
		}

		public long getHandleCount() {
			return this.handler.getHandleCountLong();
		}

		public long getErrorCount() {
			return this.handler.getErrorCountLong();
		}

		public double getMeanDuration() {
			return this.handler.getMeanDuration();
		}

		public double getMinDuration() {
			return this.handler.getMinDuration();
		}

		public double getMaxDuration() {
			return this.handler.getMaxDuration();
		}

		public double getStandardDeviationDuration() {
			return this.handler.getStandardDeviationDuration();
		}

		public long getActiveCount() {
			return this.handler.getActiveCountLong();
		}

		public org.springframework.integration.support.management.Statistics getDuration() {
			return this.handler.getDuration();
		}

		public boolean isStatsEnabled() {
			return this.handler.isStatsEnabled();
		}

		public boolean isCountsEnabled() {
			return this.handler.isCountsEnabled();
		}

	}

}

