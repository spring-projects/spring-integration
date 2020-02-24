/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * An {@link IntegrationComponentSpec} for configuring sub-flow subscribers on the
 * provided {@link BroadcastCapableChannel}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.3
 */
public class BroadcastPublishSubscribeSpec
		extends IntegrationComponentSpec<BroadcastPublishSubscribeSpec, BroadcastCapableChannel>
		implements ComponentsRegistration {

	private final Map<Object, String> subscriberFlows = new LinkedHashMap<>();

	private int order;

	protected BroadcastPublishSubscribeSpec(BroadcastCapableChannel channel) {
		Assert.state(channel.isBroadcast(),
				() -> "the " + channel +
						" must be in the 'broadcast' state for using from this 'BroadcastPublishSubscribeSpec'");
		this.target = channel;
	}

	/**
	 * Configure a {@link IntegrationFlow} to configure as a subscriber
	 *                   for the current {@link BroadcastCapableChannel}.
	 * @param subFlow the {@link IntegrationFlow} to configure as a subscriber
	 *                   for the current {@link BroadcastCapableChannel}.
	 * @return the current spec
	 */
	public BroadcastPublishSubscribeSpec subscribe(IntegrationFlow subFlow) {
		Assert.notNull(subFlow, "'subFlow' must not be null");

		IntegrationFlowBuilder flowBuilder =
				IntegrationFlows.from(this.target)
						.bridge(consumer -> consumer.order(this.order++));

		MessageChannel subFlowInput = subFlow.getInputChannel();

		if (subFlowInput == null) {
			subFlow.configure(flowBuilder);
		}
		else {
			flowBuilder.channel(subFlowInput);
		}
		this.subscriberFlows.put(flowBuilder.get(), null);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return this.subscriberFlows;
	}

}
