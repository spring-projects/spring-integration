/*
 * Copyright 2001-2019 the original author or authors.
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

import java.io.IOException;

import javax.net.ssl.SSLSession;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * An abstraction over {@link java.net.Socket} and {@link java.nio.channels.SocketChannel}
 * that sends {@link Message} objects by serializing the payload and streaming it to the
 * destination. Requires a {@link TcpListener} to receive incoming messages.
 *
 * @author Gary Russell
 * @since 2.0
 *
 */
public interface TcpConnection extends Runnable {

	/**
	 * Closes the connection.
	 */
	void close();

	/**
	 * @return true if the connection is open.
	 */
	boolean isOpen();

	/**
	 * Converts and sends the message.
	 * @param message The message,
	 */
	void send(Message<?> message);

	/**
	 * Uses the deserializer to obtain the message payload
	 * from the connection's input stream.
	 * @return The payload.
	 */
	@Nullable
	Object getPayload();

	/**
	 * @return the host name
	 */
	String getHostName();

	/**
	 * @return the host address
	 */
	String getHostAddress();

	/**
	 * @return the port
	 */
	int getPort();

	/**
	 * @return a string uniquely representing a connection.
	 */
	String getConnectionId();

	/**
	 *
	 * @return True if connection is used once.
	 */
	boolean isServer();

	/**
	 *
	 * @return the deserializer
	 */
	Deserializer<?> getDeserializer();

	/**
	 *
	 * @return the serializer
	 */
	Serializer<?> getSerializer();

	/**
	 * @return this connection's listener
	 */
	@Nullable
	TcpListener getListener();

	/**
	 * @return the next sequence number for a message received on this socket
	 */
	long incrementAndGetConnectionSequence();

	/**
	 * @return a key that can be used to reference state in a {@link Deserializer} that
	 * maintains state for this connection. Currently, this would be the InputStream
	 * associated with the connection, but the object should be treated as opaque
	 * and ONLY used as a key.
	 */
	@Nullable
	Object getDeserializerStateKey();

	/**
	 * @return the {@link SSLSession} associated with this connection, if SSL is in use,
	 * null otherwise.
	 * @since 4.2
	 */
	@Nullable
	SSLSession getSslSession();

	/**
	 * Provides getters for {@link java.net.Socket} properties.
	 * @return the socketInfo - may be null, for example in interceptors; interceptors
	 * should override and delegate to the actual TcpConnection.
	 * @since 4.3
	 */
	SocketInfo getSocketInfo();

	/**
	 * Set the connection's input stream to end of stream.
	 * @throws IOException an IO Exception.
	 * @since 5.2
	 */
	@SuppressWarnings("unused")
	default void shutdownInput() throws IOException {
		throw new UnsupportedOperationException("This connection does not support shutDownInput()");
	}

	/**
	 * Disable the socket's output stream.
	 * @throws IOException an IO Exception
	 * @since 5.2
	 */
	@SuppressWarnings("unused")
	default void shutdownOutput() throws IOException {
		throw new UnsupportedOperationException("This connection does not support shutDownOutput()");
	}

}
