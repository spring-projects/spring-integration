/*
 * Copyright 2002-present the original author or authors.
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
