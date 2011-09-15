/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.ip;

/**
 * Headers for Messages mapped from IP datagram packets.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Dave Syer
 * @since 2.0
 */
public abstract class IpHeaders {

	private static final String IP = "ip_";

	private static final String TCP = IP + "tcp_";

	public static final String HOSTNAME = IP + "hostname";

	public static final String IP_ADDRESS = IP + "address"; 

	public static final String ACK_ADDRESS = IP + "ackTo";

	public static final String ACK_ID = IP + "ackId";

	public static final String REMOTE_PORT = TCP + "remote_port";

	public static final String CONNECTION_ID = IP + "connection_id";

	/**
	 * Use apply-sequence and sequenceNumber instead
	 * @deprecated
	 */
	@Deprecated
	public static final String CONNECTION_SEQ = IP + "connection_seq";

}
