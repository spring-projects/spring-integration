/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.ip.tcp.connection.support;

import java.nio.channels.SocketChannel;

import org.springframework.integration.ip.tcp.connection.TcpNioConnection;

/**
 * Used by NIO connection factories to instantiate a {@link TcpNioConnection} object.
 * Implementations for SSL and non-SSL {@link TcpNioConnection}s are provided.
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface TcpNioConnectionSupport {

	TcpNioConnection createNewConnection(SocketChannel socketChannel,
			boolean server, boolean lookupHost) throws Exception;

}
