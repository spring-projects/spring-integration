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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.commons.serializer.Deserializer;
import org.springframework.commons.serializer.Serializer;
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

	protected TcpSender sender;

	protected boolean singleUse;

	protected final boolean server;

	protected String connectionId;
	
	private AtomicLong sequence = new AtomicLong();
	
	public AbstractTcpConnection(boolean server) {
		this.server = server;
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
