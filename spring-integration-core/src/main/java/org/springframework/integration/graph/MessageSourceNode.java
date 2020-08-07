/*
 * Copyright 2016-2020 the original author or authors.
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

import org.springframework.integration.core.MessageSource;
import org.springframework.lang.Nullable;

/**
 * Represents a message source.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class MessageSourceNode extends ErrorCapableEndpointNode implements ReceiveCountersAware {

	private Supplier<ReceiveCounters> receiveCounters;

	public MessageSourceNode(int nodeId, String name, MessageSource<?> messageSource, String output, String errors) {
		super(nodeId, name, messageSource, output, errors);
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

