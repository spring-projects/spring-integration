/*
 * Copyright © 2001 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2001-present the original author or authors.
 */

package org.springframework.integration.ip;

/**
 * @author Gary Russell
 * @since 2.0
 */
public interface CommonSocketOptions {

	/**
	 * @param soTimeout The timeout.
	 * @see java.net.Socket#setSoTimeout(int)
	 * @see java.net.DatagramSocket#setSoTimeout(int)
	 */
	void setSoTimeout(int soTimeout);

	/**
	 * @param soReceiveBufferSize The receive buffer size.
	 * @see java.net.Socket#setReceiveBufferSize(int)
	 * @see java.net.DatagramSocket#setReceiveBufferSize(int)
	 */
	void setSoReceiveBufferSize(int soReceiveBufferSize);

	/**
	 * @param soSendBufferSize The send buffer size.
	 * @see java.net.Socket#setSendBufferSize(int)
	 * @see java.net.DatagramSocket#setSendBufferSize(int)
	 */
	void setSoSendBufferSize(int soSendBufferSize);

	/**
	 * On a multi-homed system, specifies the ip address of the network interface used to communicate.
	 * For inbound adapters and gateways, specifies the interface used to listed for incoming connections.
	 * If omitted, the endpoint will listen on all available adapters. For the UDP multicast outbound adapter
	 * specifies the interface to which multicast packets will be sent. For UDP unicast and multicast
	 * adapters, specifies which interface to which the acknowledgment socket will be bound. Does not
	 * apply to TCP outbound adapters and gateways.
	 *
	 * @param localAddress The local address.
	 */
	void setLocalAddress(String localAddress);

}
