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

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.core.AmqpTemplate;

/**
 * Spec for an outbound AMQP channel adapter
 *
 * @author Gary Russell
 * @author Artme Bilan
 *
 * @since 5.3
 *
 */
public class AmqpOutboundChannelAdapterSpec extends AmqpOutboundEndpointSpec<AmqpOutboundChannelAdapterSpec> {

	protected AmqpOutboundChannelAdapterSpec(AmqpTemplate amqpTemplate) {
		super(amqpTemplate, false);
	}

	/**
	 * If true, and the message payload is an {@link Iterable} of {@link org.springframework.messaging.Message},
	 * send the messages in a single invocation of the template (same channel) and optionally
	 * wait for the confirms or die.
	 * @param multiSend true to send multiple messages.
	 * @return the spec.
	 */
	public AmqpOutboundChannelAdapterSpec multiSend(boolean multiSend) {
		this.target.setMultiSend(multiSend);
		return this;
	}

}
