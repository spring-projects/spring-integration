/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.udp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import org.springframework.messaging.MessagingException;

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
 */
public class MulticastReceivingChannelAdapter extends UnicastReceivingChannelAdapter {

	private final String group;

	/**
	 * Constructs a MulticastReceivingChannelAdapter that listens for packets on the
	 * specified multichannel address (group) and port.
	 * @param group The multichannel address.
	 * @param port The port.
	 */
	public MulticastReceivingChannelAdapter(String group, int port) {
		super(port);
		this.group = group;
	}

	/**
	 * Constructs a MulticastReceivingChannelAdapter that listens for packets on the
	 * specified multichannel address (group) and port. Enables setting the lengthCheck
	 * option, which expects a length to precede the incoming packets.
	 * @param group The multichannel address.
	 * @param port The port.
	 * @param lengthCheck If true, enables the lengthCheck Option.
	 */
	public MulticastReceivingChannelAdapter(String group, int port, boolean lengthCheck) {
		super(port, lengthCheck);
		this.group = group;
	}

	@Override
	public DatagramSocket getSocket() {
		this.lock.lock();
		try {
			if (getTheSocket() == null) {
				try {
					int port = getPort();
					MulticastSocket socket = port == 0 ? new MulticastSocket() : new MulticastSocket(port);
					String localAddress = getLocalAddress();
					if (localAddress != null) {
						socket.setNetworkInterface(
								NetworkInterface.getByInetAddress(InetAddress.getByName(localAddress)));
					}
					setSocketAttributes(socket);
					socket.joinGroup(new InetSocketAddress(this.group, 0), null);
					setSocket(socket);
				}
				catch (IOException e) {
					throw new MessagingException("failed to create DatagramSocket", e);
				}
			}
			return super.getSocket();
		}
		finally {
			this.lock.unlock();
		}
	}

}
