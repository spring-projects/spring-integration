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

package org.springframework.integration.ip.dsl;

import java.util.function.Function;

import org.springframework.messaging.Message;

/**
 * Factory methods for UDP.
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
public final class Udp {

	private Udp() {
	}

	/**
	 * Create an inbound unicast channel adapter using the supplied port.
	 * @param port the port.
	 * @return the spec.
	 */
	public static UdpInboundChannelAdapterSpec inboundAdapter(int port) {
		return new UdpInboundChannelAdapterSpec(port);
	}

	/**
	 * Create an inbound multicast channel adapter using the supplied port and
	 * group.
	 * @param port the port.
	 * @param multicastGroup the group.
	 * @return the spec.
	 */
	public static UdpInboundChannelAdapterSpec inboundMulticastAdapter(int port, String multicastGroup) {
		return new UdpInboundChannelAdapterSpec(port, multicastGroup);
	}

	/**
	 * Create an outbound unicast channel adapter using the supplied host and port.
	 * @param host the host.
	 * @param port the port.
	 * @return the spec.
	 */
	public static UdpUnicastOutboundChannelAdapterSpec outboundAdapter(String host, int port) {
		return new UdpUnicastOutboundChannelAdapterSpec(host, port);
	}

	/**
	 * Create an outbound unicast channel adapter using the supplied destination
	 * expression.
	 * @param destinationExpression destination expression.
	 * @return the spec.
	 */
	public static UdpUnicastOutboundChannelAdapterSpec outboundAdapter(String destinationExpression) {
		return new UdpUnicastOutboundChannelAdapterSpec(destinationExpression);
	}

	/**
	 * Create an outbound unicast channel adapter using the supplied destination
	 * expression.
	 * @param destinationFunction function that will provide the destination based on the message.
	 * @return the spec.
	 */
	public static UdpUnicastOutboundChannelAdapterSpec outboundAdapter(Function<Message<?>, ?> destinationFunction) {
		return new UdpUnicastOutboundChannelAdapterSpec(destinationFunction);
	}

	/**
	 * Create an outbound multicast channel adapter using the supplied host and port.
	 * @param host the host.
	 * @param port the port.
	 * @return the spec.
	 */
	public static UdpMulticastOutboundChannelAdapterSpec outboundMulticastAdapter(String host, int port) {
		return new UdpMulticastOutboundChannelAdapterSpec(host, port);
	}

	/**
	 * Create an outbound multicast channel adapter using the supplied destination
	 * expression.
	 * @param destinationExpression destination expression.
	 * @return the spec.
	 */
	public static UdpMulticastOutboundChannelAdapterSpec outboundMulticastAdapter(String destinationExpression) {
		return new UdpMulticastOutboundChannelAdapterSpec(destinationExpression);
	}

	/**
	 * Create an outbound multicast channel adapter using the supplied destination
	 * expression.
	 * @param destinationFunction function that will provide the destination based on the message.
	 * @return the spec.
	 */
	public static UdpMulticastOutboundChannelAdapterSpec outboundMulticastAdapter(
			Function<Message<?>, ?> destinationFunction) {

		return new UdpMulticastOutboundChannelAdapterSpec(destinationFunction);
	}

}
