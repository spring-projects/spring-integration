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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.util.Assert;

/**
 * Base class for all connection factories.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public abstract class AbstractConnectionFactory 
		implements ConnectionFactory, Runnable, SmartLifecycle  {

	protected Log logger = LogFactory.getLog(this.getClass());
	
	protected final static int DEFAULT_REPLY_TIMEOUT = 10000;
	
	protected String host;
	
	protected int port;
	
	protected TcpListener listener;

	protected TcpSender sender;

	protected int soTimeout;

	private int soSendBufferSize;

	private int soReceiveBufferSize;
	
	private boolean soTcpNoDelay;

	private int soLinger  = -1; // don't set by default

	private boolean soKeepAlive;

	private int soTrafficClass = -1; // don't set by default
	
	protected Executor taskExecutor;
	
	protected Deserializer<?> deserializer = new ByteArrayCrLfSerializer();
	
	protected Serializer<?> serializer = new ByteArrayCrLfSerializer();
	
	protected TcpMessageMapper mapper = new TcpMessageMapper();

	protected boolean singleUse;

	protected int poolSize = 5;

	protected boolean active;

	protected TcpConnectionInterceptorFactoryChain interceptorFactoryChain;

	/**
	 * Sets socket attributes on the socket.
	 * @param socket The socket.
	 * @throws SocketException
	 */
	protected void setSocketAttributes(Socket socket) throws SocketException {
		if (this.soTimeout >= 0) {
			socket.setSoTimeout(this.soTimeout);
		}
		if (this.soSendBufferSize > 0) {
			socket.setSendBufferSize(this.soSendBufferSize);
		}
		if (this.soReceiveBufferSize > 0) {
			socket.setReceiveBufferSize(this.soReceiveBufferSize);
		}
		socket.setTcpNoDelay(this.soTcpNoDelay);
		if (this.soLinger >= 0) {
			socket.setSoLinger(true, this.soLinger);
		}
		if (this.soTrafficClass >= 0) {
			socket.setTrafficClass(this.soTrafficClass);
		}
		socket.setKeepAlive(this.soKeepAlive);
	}

	/**
	 * @return the soTimeout
	 */
	public int getSoTimeout() {
		return soTimeout;
	}

	/**
	 * @param soTimeout the soTimeout to set
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @return the soReceiveBufferSize
	 */
	public int getSoReceiveBufferSize() {
		return soReceiveBufferSize;
	}

	/**
	 * @param soReceiveBufferSize the soReceiveBufferSize to set
	 */
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/**
	 * @return the soSendBufferSize
	 */
	public int getSoSendBufferSize() {
		return soSendBufferSize;
	}

	/**
	 * @param soSendBufferSize the soSendBufferSize to set
	 */
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.soSendBufferSize = soSendBufferSize;
	}

	/**
	 * @return the soTcpNoDelay
	 */
	public boolean isSoTcpNoDelay() {
		return soTcpNoDelay;
	}

	/**
	 * @param soTcpNoDelay the soTcpNoDelay to set
	 */
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		this.soTcpNoDelay = soTcpNoDelay;
	}

	/**
	 * @return the soLinger
	 */
	public int getSoLinger() {
		return soLinger;
	}

	/**
	 * @param soLinger the soLinger to set
	 */
	public void setSoLinger(int soLinger) {
		this.soLinger = soLinger;
	}

	/**
	 * @return the soKeepAlive
	 */
	public boolean isSoKeepAlive() {
		return soKeepAlive;
	}

	/**
	 * @param soKeepAlive the soKeepAlive to set
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @return the soTrafficClass
	 */
	public int getSoTrafficClass() {
		return soTrafficClass;
	}

	/**
	 * @param soTrafficClass the soTrafficClass to set
	 */
	public void setSoTrafficClass(int soTrafficClass) {
		this.soTrafficClass = soTrafficClass;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Registers a TcpListener to receive messages after
	 * the payload has been converted from the input data.
	 * @param listener the TcpListener.
	 */
	public void registerListener(TcpListener listener) {
		Assert.isNull(this.listener, this.getClass().getName() +
				" may only be used by one inbound adapter");
		this.listener = listener;
	}

	/**
	 * Registers a TcpSender; for server sockets, used to 
	 * provide connection information so a sender can be used
	 * to reply to incoming messages.
	 * @param sender The sender
	 */
	public void registerSender(TcpSender sender) {
		Assert.isNull(this.sender, this.getClass().getName() +
				" may only be used by one outbound adapter");
		this.sender = sender;
	}

	/**
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 
	 * @param deserializer the deserializer to set
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 * 
	 * @param serializer the serializer to set
	 */
	public void setSerializer(Serializer<?> serializer) {
		this.serializer = serializer;
	}

	/**
	 * 
	 * @param mapper the mapper to set; defaults to a {@link TcpMessageMapper}
	 */
	public void setMapper(TcpMessageMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * @return the singleUse
	 */
	public boolean isSingleUse() {
		return singleUse;
	}

	/**
	 * If true, sockets created by this factory will be used once.
	 * @param singleUse
	 */
	public void setSingleUse(boolean singleUse) {
		this.singleUse = singleUse;
	}

	
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public void setInterceptorFactoryChain(TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		this.interceptorFactoryChain = interceptorFactoryChain;
	}

	/**
	 * Closes the server.
	 */
	public abstract void close();

	/**
	 * Creates a taskExecutor (if one was not provided) and starts
	 * the listening process on one of its threads.
	 */
	public void start() {
		if (this.taskExecutor == null) {
			this.taskExecutor = Executors.newFixedThreadPool(this.poolSize);
		}
		this.active = true;
		this.taskExecutor.execute(this);
	}

	/**
	 * Stops the server.
	 */
	public void stop() {
		this.active = false;
		this.close();
	}

	protected TcpConnection wrapConnection(TcpConnection connection) throws Exception {
		if (this.interceptorFactoryChain == null) {
			return connection;
		}
		TcpConnectionInterceptorFactory[] interceptorFactories = 
			this.interceptorFactoryChain.getInterceptorFactories();
		if (interceptorFactories == null) {
			return connection;
		}
		for (TcpConnectionInterceptorFactory factory : interceptorFactories) {
			TcpConnectionInterceptor wrapper = factory.getInterceptor();
			wrapper.setTheConnection(connection);
			// if no ultimate listener or sender, register each wrapper in turn
			if (this.listener == null) {
				connection.registerListener(wrapper);
			}
			if (this.sender == null) {
				connection.registerSender(wrapper);
			}
			connection = wrapper;
		}
		return connection;
	}

	/**
	 * 
	 * Times out any expired connections then, if selectionCount > 0, processes the selected keys.
	 *  
	 * @param selectionCount
	 * @param selector
	 * @param connections
	 * @throws IOException
	 */
	protected void processNioSelections(int selectionCount, final Selector selector, ServerSocketChannel server,
			Map<SocketChannel, TcpNioConnection> connections) throws IOException {
		long now = 0;
		if (this.soTimeout > 0) {
			Iterator<SocketChannel> it = connections.keySet().iterator();
			now = System.currentTimeMillis();
			while (it.hasNext()) {
				SocketChannel channel = it.next();
				if (!channel.isOpen()) {
					logger.debug("Removing closed channel");
					it.remove();
				} else {
					TcpNioConnection connection = connections.get(channel);
					if (now - connection.getLastRead() > this.soTimeout) {
						logger.warn("Timing out TcpNioConnection " +
									this.port + " : " +
								    connection.getConnectionId());
						connection.timeout();
					}
				}
			}
		}
		if (logger.isTraceEnabled()) {
			if (host == null) {
				logger.trace("Port " + this.port + " SelectionCount: " + selectionCount);
			} else {
				logger.trace("Host " + this.host + " port " + this.port + " SelectionCount: " + selectionCount);
			}
		}
		if (selectionCount > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				final SelectionKey key = iterator.next();
				iterator.remove();
				if (!key.isValid()) {
					logger.debug("Selection key no longer valid");
				}
				else if (key.isReadable()) {
					try {
						key.interestOps(key.interestOps() - key.readyOps());
						final TcpNioConnection connection; 
						connection = (TcpNioConnection) key.attachment();
						connection.setLastRead(System.currentTimeMillis());
						this.taskExecutor.execute(new Runnable() {
							public void run() {
								try {
									connection.readPacket();
								} catch (Exception e) {
									if (connection.isOpen()) {
										logger.error("Exception on read " +
												connection.getConnectionId() + " " +
												e.getMessage());
										connection.close();
									} else {
										logger.debug("Connection closed");
									}
								}
								if (key.channel().isOpen()) {
									key.interestOps(SelectionKey.OP_READ);
									selector.wakeup();
								}
							}});
					} catch (Exception e) {
						if (e instanceof CancelledKeyException) {
							logger.debug("Exception on readable key", e);
							continue;
						}
						logger.error("Exception on readable key", e);
					}
				}
				else if (key.isAcceptable()) {
					try {
						doAccept(selector, server, now);
					} catch (Exception e) {
						logger.error("Exception accepting new connection", e);
					}
				}
				else {
					logger.error("Unexpected key: " + key);
				}
			}
		}
	}

	/**
	 * @param selector
	 * @param now
	 * @throws IOException
	 */
	protected void doAccept(final Selector selector, ServerSocketChannel server, long now) throws IOException {
		throw new UnsupportedOperationException("Nio server factory must override this method");
	}
	
	public int getPhase() {
		return 0;
	}

	public boolean isAutoStartup() {
		return true;
	}

	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

}
