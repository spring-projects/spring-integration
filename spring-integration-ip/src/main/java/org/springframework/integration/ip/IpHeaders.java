/*
 * Copyright 2002-2022 the original author or authors.
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
 * Headers for Messages mapped from IP datagram packets.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public final class IpHeaders {

	/**
	 * The {@value IP_PREFIX} prefix for UDP and TCP headers.
	 */
	public static final String IP_PREFIX = "ip_";

	/**
	 * The {@value TCP_PREFIX} prefix for TCP headers.
	 */
	private static final String TCP_PREFIX = IP_PREFIX + "tcp_";

	/**
	 * The host name from which a TCP message or UDP packet was received. If
	 * {@code lookupHost} is {@code false}, this will contain the ip address.
	 */
	public static final String HOSTNAME = IP_PREFIX + "hostname";

	/**
	 * The ip address from which a TCP message or UDP packet was received.
	 */
	public static final String IP_ADDRESS = IP_PREFIX + "address";

	/**
	 * The remote port for a UDP packet.
	 */
	public static final String PORT = IP_PREFIX + "port";

	/**
	 * The remote address for a UDP packet.
	 */
	public static final String PACKET_ADDRESS = IP_PREFIX + "packetAddress";

	/**
	 * The remote ip address to which UDP application-level acks will be sent. The
	 * framework includes acknowledgment information in the data packet.
	 */
	public static final String ACK_ADDRESS = IP_PREFIX + "ackTo";

	/**
	 * A correlation id for UDP application-level acks. The
	 * framework includes acknowledgment information in the data packet.
	 */
	public static final String ACK_ID = IP_PREFIX + "ackId";

	/**
	 * A unique identifier for a TCP connection; set by the framework for
	 * inbound messages; when sending to a server-side inbound
	 * channel adapter, or replying to an inbound gateway, this header is
	 * required so the endpoint can determine which connection to send
	 * the message to.
	 */
	public static final String CONNECTION_ID = IP_PREFIX + "connectionId";

	/**
	 * For information only - when using a cached or failover client connection
	 * factory, contains the actual underlying connection id.
	 */
	public static final String ACTUAL_CONNECTION_ID = IP_PREFIX + "actualConnectionId";

	/**
	 * The local address (InetAddress) that the socket is connected to.
	 * @since 4.2.5.
	 */
	public static final String LOCAL_ADDRESS = IP_PREFIX + "localInetAddress";

	/**
	 * The remote port from which a TCP message was received.
	 */
	public static final String REMOTE_PORT = TCP_PREFIX + "remotePort";

	private IpHeaders() {
	}

}
