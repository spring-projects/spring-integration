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
 * Channel adapter that joins a multicast group and receives incoming packets and
 * sends them to an output channel.
 *
 * @author Gary Russell
 * @author Marcin Pilaczynski
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.udp.inbound.MulticastReceivingChannelAdapter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class MulticastReceivingChannelAdapter
		extends org.springframework.integration.ip.udp.inbound.MulticastReceivingChannelAdapter {

	/**
	 * Construct a MulticastReceivingChannelAdapter that listens for packets on the
	 * specified multichannel address (group) and port.
	 * @param group The multichannel address.
	 * @param port The port.
	 */
	public MulticastReceivingChannelAdapter(String group, int port) {
		super(group, port);
	}

	/**
	 * Construct a MulticastReceivingChannelAdapter that listens for packets on the
	 * specified multichannel address (group) and port. Enables setting the lengthCheck
	 * option, which expects a length to precede the incoming packets.
	 * @param group The multichannel address.
	 * @param port The port.
	 * @param lengthCheck If true, enables the lengthCheck Option.
	 */
	public MulticastReceivingChannelAdapter(String group, int port, boolean lengthCheck) {
		super(group, port, lengthCheck);
	}

}
