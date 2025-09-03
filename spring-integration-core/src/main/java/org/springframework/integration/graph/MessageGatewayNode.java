/*
 * Copyright 2016-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.integration.gateway.MessagingGatewaySupport;

/**
 * Represents an inbound gateway.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class MessageGatewayNode extends ErrorCapableEndpointNode implements SendTimersAware {

	private @Nullable Supplier<SendTimers> sendTimers;

	public MessageGatewayNode(int nodeId, String name, MessagingGatewaySupport gateway, @Nullable String output,
			@Nullable String errors) {

		super(nodeId, name, gateway, output, errors);
	}

	@Override
	public void sendTimers(Supplier<SendTimers> timers) {
		this.sendTimers = timers;
	}

	public @Nullable SendTimers getSendTimers() {
		return this.sendTimers != null ? this.sendTimers.get() : null;
	}

}
