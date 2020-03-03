/*
 * Copyright 2017-2020 the original author or authors.
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

import java.net.Socket;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;


/**
 * Used by NET connection factories to instantiate a {@link TcpNetConnection} object.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
@FunctionalInterface
public interface TcpNetConnectionSupport {

	/**
	 * Create a new {@link TcpNetConnection} object wrapping the {@link Socket}.
	 * @param socket the Socket.
	 * @param server true if this connection is a server connection.
	 * @param lookupHost true if hostname lookup should be performed, otherwise the connection will
	 * be identified using the ip address.
	 * @param applicationEventPublisher the publisher to which OPEN, CLOSE and EXCEPTION events will
	 * be sent; may be null if event publishing is not required.
	 * @param connectionFactoryName the name of the connection factory creating this connection; used
	 * during event publishing, may be null, in which case "unknown" will be used.
	 * @return the TcpNetConnection
	 */
	TcpNetConnection createNewConnection(Socket socket,
			boolean server, boolean lookupHost,
			@Nullable ApplicationEventPublisher applicationEventPublisher,
			String connectionFactoryName);

}
