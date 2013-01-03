/*
 * Copyright 2001-2013 the original author or authors.
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

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.Message;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.util.Assert;

/**
 * Base class for TcpConnections. TcpConnections are established by
 * client connection factories (outgoing) or server connection factories
 * (incoming).
 *
 * @author Gary Russell
 * @since 2.0
 *
 */
public abstract class TcpConnectionSupport implements TcpConnection {

	protected final Log logger = LogFactory.getLog(this.getClass());

	@SuppressWarnings("rawtypes")
	private volatile Deserializer deserializer;

	@SuppressWarnings("rawtypes")
	private volatile Serializer serializer;

	private volatile TcpMessageMapper mapper;

	private volatile TcpListener listener;

	private volatile TcpListener actualListener;

	private volatile TcpSender sender;

	private volatile boolean singleUse;

	private final boolean server;

	private volatile String connectionId;

	private final AtomicLong sequence = new AtomicLong();

	private volatile int soLinger = -1;

	private volatile String hostName = "unknown";

	private volatile String hostAddress = "unknown";

	public TcpConnectionSupport() {
		server = false;
	}

	public TcpConnectionSupport(Socket socket, boolean server, boolean lookupHost) {
		this.server = server;
		InetAddress inetAddress = socket.getInetAddress();
		if (inetAddress != null) {
			this.hostAddress = inetAddress.getHostAddress();
			if (lookupHost) {
				this.hostName = inetAddress.getHostName();
			} else {
				this.hostName = this.hostAddress;
			}
		}
		int port = socket.getPort();
		this.connectionId = this.hostName + ":" + port + ":" + UUID.randomUUID().toString();
		try {
			this.soLinger = socket.getSoLinger();
		} catch (SocketException e) { }
	}

	public void afterSend(Message<?> message) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Message sent " + message);
		}
		if (this.singleUse) {
			// if (we're a server socket, or a send-only socket), and soLinger <> 0, close
			if ((this.isServer() || this.actualListener == null) && this.soLinger != 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("Closing single-use connection" + this.getConnectionId());
				}
				this.closeConnection();
			}
		}
	}

	/**
	 * Closes this connection.
	 */
	public void close() {
		if (this.sender != null) {
			this.sender.removeDeadConnection(this);
		}
	}

	/**
	 * If we have been intercepted, propagate the close from the outermost interceptor;
	 * otherwise, just call close().
	 */
	protected void closeConnection() {
		if (!(this.listener instanceof TcpConnectionInterceptor)) {
			close();
			return;
		}
		TcpConnectionInterceptor outerInterceptor = (TcpConnectionInterceptor) this.listener;
		while (outerInterceptor.getListener() instanceof TcpConnectionInterceptor) {
			outerInterceptor = (TcpConnectionInterceptor) outerInterceptor.getListener();
		}
		outerInterceptor.close();
	}

	/**
	 * @return the mapper
	 */
	public TcpMessageMapper getMapper() {
		return mapper;
	}

	/**
	 * @param mapper the mapper to set
	 */
	public void setMapper(TcpMessageMapper mapper) {
		Assert.notNull(mapper, this.getClass().getName() + " Mapper may not be null");
		this.mapper = mapper;
		if (this.serializer != null &&
			 !(this.serializer instanceof AbstractByteArraySerializer)) {
			mapper.setStringToBytes(false);
		}
	}

	/**
	 *
	 * @return the deserializer
	 */
	public Deserializer<?> getDeserializer() {
		return this.deserializer;
	}

	/**
	 * @param deserializer the deserializer to set
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 *
	 * @return the serializer
	 */
	public Serializer<?> getSerializer() {
		return this.serializer;
	}

	/**
	 * @param serializer the serializer to set
	 */
	public void setSerializer(Serializer<?> serializer) {
		this.serializer = serializer;
		if (!(serializer instanceof AbstractByteArraySerializer)) {
			this.mapper.setStringToBytes(false);
		}
	}

	/**
	 * Sets the listener that will receive incoming Messages.
	 * @param listener The listener.
	 */
	public void registerListener(TcpListener listener) {
		this.listener = listener;
		// Determine the actual listener for this connection
		if (!(this.listener instanceof TcpConnectionInterceptor)) {
			this.actualListener = this.listener;
		} else {
			TcpConnectionInterceptor outerInterceptor = (TcpConnectionInterceptor) this.listener;
			while (outerInterceptor.getListener() instanceof TcpConnectionInterceptor) {
				outerInterceptor = (TcpConnectionInterceptor) outerInterceptor.getListener();
			}
			this.actualListener = outerInterceptor.getListener();
		}
	}

	/**
	 * Registers a sender. Used on server side connections so a
	 * sender can determine which connection to send a reply
	 * to.
	 * @param sender the sender.
	 */
	public void registerSender(TcpSender sender) {
		this.sender = sender;
		if (sender != null) {
			sender.addNewConnection(this);
		}
	}

	/**
	 * @return the listener
	 */
	public TcpListener getListener() {
		return this.listener;
	}

	/**
	 * @return the sender
	 */
	public TcpSender getSender() {
		return sender;
	}

	/**
	 * @param singleUse true if this socket is to used once and
	 * discarded.
	 */
	public void setSingleUse(boolean singleUse) {
		this.singleUse = singleUse;
	}

	/**
	 *
	 * @return True if connection is used once.
	 */
	public boolean isSingleUse() {
		return this.singleUse;
	}

	public boolean isServer() {
		return server;
	}

	public long incrementAndGetConnectionSequence() {
		return this.sequence.incrementAndGet();
	}

	public String getHostAddress() {
		return this.hostAddress;
	}

	public String getHostName() {
		return this.hostName;
	}

	public String getConnectionId() {
		return this.connectionId;
	}

}
