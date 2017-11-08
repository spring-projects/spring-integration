/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import java.util.function.Function;

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.ip.udp.MulticastSendingMessageHandler;
import org.springframework.messaging.Message;

/**
 * A {@link MessageHandlerSpec} for {@link MulticastSendingMessageHandler}s.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class UdpMulticastOutboundChannelAdapterSpec
		extends AbstractUdpOutboundChannelAdapterSpec<UdpMulticastOutboundChannelAdapterSpec> {

	UdpMulticastOutboundChannelAdapterSpec(String host, int port) {
		this.target = new MulticastSendingMessageHandler(host, port);
	}

	UdpMulticastOutboundChannelAdapterSpec(String destinationExpression) {
		this.target = new MulticastSendingMessageHandler(destinationExpression);
	}

	UdpMulticastOutboundChannelAdapterSpec(Function<Message<?>, ?> destinationFunction) {
		this.target = new MulticastSendingMessageHandler(new FunctionExpression<>(destinationFunction));
	}

	/**
	 * @param timeToLive the timeToLive.
	 * @return the spec.
	 * @see MulticastSendingMessageHandler#setTimeToLive(int)
	 */
	public UdpMulticastOutboundChannelAdapterSpec timeToLive(int timeToLive) {
		((MulticastSendingMessageHandler) this.target).setTimeToLive(timeToLive);
		return _this();
	}

}
