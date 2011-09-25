/*
 * Copyright 2001-2011 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.Message;

/**
 * An abstraction over {@link Socket} and {@link SocketChannel} that
 * sends {@link Message} objects by serializing the payload
 * and streaming it to the destination. Requires a {@link TcpListener}
 * to receive incoming messages.
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
	 * @param message The message
	 * @throws Exception
	 */
	void send(Message<?> message) throws Exception;

	/**
	 * Uses the deserializer to obtain the message payload
	 * from the connection's input stream.
	 * @return The payload
	 * @throws Exception
	 */
	Object getPayload() throws Exception;

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
	 * Sets the listener that will receive incoming Messages.
	 * @param listener The listener
	 */
	void registerListener(TcpListener listener);

	/**
	 * Registers a sender. Used on server side sockets so a
	 * sender can determine which connection to send a reply
	 * to.
	 * @param sender the sender
	 */
	void registerSender(TcpSender sender);
	
	/**
	 * @return a string uniquely representing a connection.
	 */
	String getConnectionId();
	
	/**
	 * When true, the socket is used once and discarded.
	 * @param singleUse the singleUse
	 */
	void setSingleUse(boolean singleUse);

	/**
	 *
	 * @return True if connection is used once.
	 */
	boolean isSingleUse();
	
	/**
	 *
	 * @return True if connection is used once.
	 */
	boolean isServer();
	
	/**
	 * @param mapper the mapper
	 */
	void setMapper(TcpMessageMapper mapper);

	/**
	 *
	 * @return the deserializer
	 */
	Deserializer<?> getDeserializer();

	/**
	 * @param deserializer the deserializer to set
	 */
	void setDeserializer(Deserializer<?> deserializer);

	/**
	 *
	 * @return the serializer
	 */
	Serializer<?> getSerializer();
	
	/**
	 * @param serializer the serializer to set
	 */
	void setSerializer(Serializer<?> serializer);
	
	/**
	 * @return this connection's listener
	 */
	TcpListener getListener();

	/**
	 * @return the next sequence number for a message received on this socket
	 */
	long getConnectionSeq();
	
}
