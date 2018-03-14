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

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.management.MessageSourceMetrics;

/**
 * Represents a message source.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class MessageSourceNode extends ErrorCapableEndpointNode {

	public MessageSourceNode(int nodeId, String name, MessageSource<?> messageSource, String output, String errors) {
		super(nodeId, name, messageSource, output, errors, messageSource instanceof MessageSourceMetrics
				? new Stats((MessageSourceMetrics) messageSource) : new IntegrationNode.Stats());
	}


	public static final class Stats extends IntegrationNode.Stats {

		private final MessageSourceMetrics source;

		Stats(MessageSourceMetrics source) {
			this.source = source;
		}

		@Override
		protected boolean isAvailable() {
			return this.source.isCountsEnabled();
		}

		public long getMessageCount() {
			return this.source.getMessageCountLong();
		}

	}

}

