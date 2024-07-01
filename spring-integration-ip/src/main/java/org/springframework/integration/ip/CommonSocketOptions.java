/*
 * Copyright 2001-2022 the original author or authors.
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

package org.springframework.integration.ip;

/**
 * @author Gary Russell
 * @since 2.0
 */
public interface CommonSocketOptions {

	/**
	 * @param soTimeout The timeout, in milliseconds.
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
