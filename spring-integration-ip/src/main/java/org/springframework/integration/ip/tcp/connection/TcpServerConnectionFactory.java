/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.net.SocketAddress;

/**
 * Connection factories that act as TCP servers, listening for incoming connections.
 * @author Gary Russell
 * @since 4.2
 *
 */
public interface TcpServerConnectionFactory {

	/**
	 * Return the port this server is listening on.
	 * If the factory is configured to listen on a random port (0), this
	 * will return the actual port after the factory is started. It may
	 * return the previous value if the factory is stopped.
	 * @return the port.
	 */
	int getPort();

	/**
	 * Return the {@link SocketAddress} that the underlying {@code ServerSocket}
	 * is bound to.
	 * @return the socket address.
	 */
	SocketAddress getServerSocketAddress();

}
