/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.integration.graph;

import org.springframework.integration.support.management.MessageHandlerMetrics;
import org.springframework.integration.support.management.Statistics;
import org.springframework.messaging.MessageHandler;

/**
 * Represents a message handler.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class MessageHandlerNode extends EndpointNode {

	private final String input;

	public MessageHandlerNode(int nodeId, String name, MessageHandler handler, String input, String output) {
		super(nodeId, name, handler, output, handler instanceof MessageHandlerMetrics
				? new Stats((MessageHandlerMetrics) handler) : new IntegrationNode.Stats());
		this.input = input;
	}

	public String getInput() {
		return this.input;
	}

	public static final class Stats extends IntegrationNode.Stats {

		private final MessageHandlerMetrics handler;

		Stats(MessageHandlerMetrics handler) {
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

		public Statistics getDuration() {
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

