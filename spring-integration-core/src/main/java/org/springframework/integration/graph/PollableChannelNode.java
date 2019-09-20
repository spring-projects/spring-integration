/*
 * Copyright 2019 the original author or authors.
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
import org.springframework.messaging.MessageChannel;

/**
 * Represents a pollable channel.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
public class PollableChannelNode extends MessageChannelNode implements ReceiveCountersAware {

	private Supplier<ReceiveCounters> receiveCounters;

	public PollableChannelNode(int nodeId, String name, MessageChannel channel) {
		super(nodeId, name, channel);
	}

	@Nullable
	public ReceiveCounters getReceiveCounters() {
		return this.receiveCounters != null ? this.receiveCounters.get() : null;
	}

	@Override
	public void receiveCounters(Supplier<ReceiveCounters> counters) {
		this.receiveCounters = counters;
	}

}
