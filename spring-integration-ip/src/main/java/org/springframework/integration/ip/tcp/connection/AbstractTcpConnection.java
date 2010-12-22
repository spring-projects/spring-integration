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

package org.springframework.integration.ip.tcp.connection;

import java.net.Socket;
import java.net.SocketException;
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
public abstract class AbstractTcpConnection implements TcpConnection {

	protected Log logger = LogFactory.getLog(this.getClass());
	
	@SuppressWarnings("rawtypes")
	protected Deserializer deserializer;
	
	@SuppressWarnings("rawtypes")
	protected Serializer serializer;
	
	protected TcpMessageMapper mapper;
	
	protected TcpListener listener;
	
	private TcpListener actualListener;

	protected TcpSender sender;

	protected boolean singleUse;

	protected final boolean server;

	protected String connectionId;
	
	private AtomicLong sequence = new AtomicLong();
	
	private int soLinger = -1;
	
	public AbstractTcpConnection(Socket socket, boolean server) {
		this.server = server;
		try {
			this.soLinger = socket.getSoLinger();
		} catch (SocketException e) { }
	}
	
	public void afterSend(Message<?> message) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("Message sent " + message);
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
	 * @param listener the listener to set
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
	 * @param sender the sender to set
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

	public long getConnectionSeq() {
		return sequence.incrementAndGet();
	}

	
}
