/*
 * Copyright 2002-2020 the original author or authors.
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

/**
 * An interface representing a sending client of a connection
 * factory.
 * @author Gary Russell
 * @since 2.0
 *
 */
@FunctionalInterface
public interface TcpSender {

	/**
	 * When we are using sockets owned by a {@link TcpListener}, this
	 * method is called each time a new connection is made.
	 * @param connection The connection.
	 */
	void addNewConnection(TcpConnection connection);

	/**
	 * When we are using sockets owned by a {@link TcpListener}, this
	 * method is called each time a connection is closed.
	 * @param connection The connection.
	 */
	default void removeDeadConnection(TcpConnection connection) {
	}

}
