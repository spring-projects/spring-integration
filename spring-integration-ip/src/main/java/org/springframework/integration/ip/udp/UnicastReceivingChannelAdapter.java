/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ip.udp;

/**
 * A channel adapter to receive incoming UDP packets. Packets can optionally be preceded by a
 * 4 byte length field, used to validate that all data was received. Packets may also contain
 * information indicating an acknowledgment needs to be sent.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.udp.inbound.UnicastReceivingChannelAdapter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class UnicastReceivingChannelAdapter
		extends org.springframework.integration.ip.udp.inbound.UnicastReceivingChannelAdapter {

	/**
	 * Construct a UnicastReceivingChannelAdapter that listens on the specified port.
	 * @param port The port.
	 */
	public UnicastReceivingChannelAdapter(int port) {
		super(port);
	}

	/**
	 * Construct a UnicastReceivingChannelAdapter that listens for packets on
	 * the specified port. Enables setting the lengthCheck option, which expects
	 * a length to precede the incoming packets.
	 * @param port The port.
	 * @param lengthCheck If true, enables the lengthCheck Option.
	 */
	public UnicastReceivingChannelAdapter(int port, boolean lengthCheck) {
		super(port, lengthCheck);
	}

}
