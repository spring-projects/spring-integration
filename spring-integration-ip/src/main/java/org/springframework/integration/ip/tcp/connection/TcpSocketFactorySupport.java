/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

/**
 * Strategy interface for supplying Socket Factories.
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface TcpSocketFactorySupport {

	/**
	 * Supplies the {@link ServerSocketFactory} to be used to create new
	 * {@link java.net.ServerSocket}s.
	 * @return the ServerSocketFactory
	 */
	ServerSocketFactory getServerSocketFactory();

	/**
	 * Supplies the {@link SocketFactory} to be used to create new
	 * {@link java.net.Socket}s.
	 * @return the SocketFactory
	 */
	SocketFactory getSocketFactory();

}
