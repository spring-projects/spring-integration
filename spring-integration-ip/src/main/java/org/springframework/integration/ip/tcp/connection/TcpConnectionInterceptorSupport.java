/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLSession;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * Base class for {@link TcpConnectionInterceptor}s; passes all method calls through
 * to the underlying {@link TcpConnection}.
 *
 * @author Gary Russell
 * @author Kazuki Shimizu
 * @author Christian Tzolov
 *
 * @since 2.0
 */
public abstract class TcpConnectionInterceptorSupport extends TcpConnectionSupport implements TcpConnectionInterceptor {

	private final Lock lock = new ReentrantLock();

	private TcpConnectionSupport theConnection;

	private TcpListener tcpListener;

	private boolean realSender;

	private List<TcpSender> interceptedSenders;

	private boolean removed;

	public TcpConnectionInterceptorSupport() {
	}

	public TcpConnectionInterceptorSupport(ApplicationEventPublisher applicationEventPublisher) {
		super(applicationEventPublisher);
	}

	@Override
	public void close() {
		this.theConnection.close();
	}

	@Override
	public boolean isOpen() {
		return this.theConnection.isOpen();
	}

	@Override
	public Object getPayload() {
		return this.theConnection.getPayload();
	}

	@Override
	public String getHostName() {
		return this.theConnection.getHostName();
	}

	@Override
	public String getHostAddress() {
		return this.theConnection.getHostAddress();
	}

	@Override
	public int getPort() {
		return this.theConnection.getPort();
	}

	@Override
	public Object getDeserializerStateKey() {
		return this.theConnection.getDeserializerStateKey();
	}

	@Override
	public void registerListener(TcpListener listener) {
		this.tcpListener = listener;
		this.theConnection.registerListener(this);
	}

	@Override
	public void registerSender(TcpSender sender) {
		this.theConnection.registerSender(this);
	}

	@Override
	public void registerSenders(List<TcpSender> sendersToRegister) {
		this.interceptedSenders = sendersToRegister;
		if (sendersToRegister.size() > 0) {
			if (!(sendersToRegister.get(0) instanceof TcpConnectionInterceptorSupport)) {
				this.realSender = true;
			}
			else {
				this.realSender = ((TcpConnectionInterceptorSupport) this.interceptedSenders.get(0))
						.hasRealSender();
			}
		}
		if (this.theConnection instanceof TcpConnectionInterceptorSupport) {
			this.theConnection.registerSenders(Collections.singletonList(this));
		}
		else {
			super.registerSender(this);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * IMPORTANT: Do not override this method in your interceptor implementation if the
	 * intercepted connection is created by a server connection factory, because the
	 * connection id of the underlying connection is used for routing when arbitrary
	 * outbound messaging is being used. The method is not final because client-side
	 * interceptors can override it without any issues.
	 */
	@Override
	public String getConnectionId() {
		return this.theConnection.getConnectionId();
	}

	@Override
	public SocketInfo getSocketInfo() {
		return this.theConnection.getSocketInfo();
	}

	@Override
	public String getConnectionFactoryName() {
		return this.theConnection.getConnectionFactoryName();
	}

	@Override
	public void run() {
		this.theConnection.run();
	}

	@Override
	public void setMapper(TcpMessageMapper mapper) {
		this.theConnection.setMapper(mapper);
	}

	@Override
	public Deserializer<?> getDeserializer() {
		return this.theConnection.getDeserializer();
	}

	@Override
	public void setDeserializer(Deserializer<?> deserializer) {
		this.theConnection.setDeserializer(deserializer);
	}

	@Override
	public Serializer<?> getSerializer() {
		return this.theConnection.getSerializer();
	}

	@Override
	public void setSerializer(Serializer<?> serializer) {
		this.theConnection.setSerializer(serializer);
	}

	@Override
	public boolean isServer() {
		return this.theConnection.isServer();
	}

	@Override
	public SSLSession getSslSession() {
		return this.theConnection.getSslSession();
	}

	@Override
	public boolean onMessage(Message<?> message) {
		if (this.tcpListener == null) {
			if (message instanceof ErrorMessage) {
				return false;
			}
			else {
				throw new NoListenerException("No listener registered for message reception");
			}
		}
		return this.tcpListener.onMessage(message);
	}

	@Override
	public void send(Message<?> message) {
		this.theConnection.send(message);
	}

	/**
	 * Return the underlying connection (or next interceptor).
	 * @return the connection
	 */
	public TcpConnectionSupport getTheConnection() {
		return this.theConnection;
	}

	/**
	 * Set the underlying connection (or next interceptor).
	 * @param theConnection the connection
	 */
	public void setTheConnection(TcpConnectionSupport theConnection) {
		this.theConnection = theConnection;
	}

	/**
	 * @return the listener
	 */
	@Override
	@Nullable
	public TcpListener getListener() {
		return this.tcpListener;
	}

	@Override
	public void addNewConnection(TcpConnection connection) {
		if (this.interceptedSenders != null) {
			this.interceptedSenders.forEach(sender -> sender.addNewConnection(this));
		}
	}

	@Override
	public void removeDeadConnection(TcpConnection connection) {
		this.lock.lock();
		try {
			if (this.removed) {
				return;
			}
			this.removed = true;
			if (this.theConnection instanceof TcpConnectionInterceptorSupport && !this.theConnection.equals(this)) {
				((TcpConnectionInterceptorSupport) this.theConnection).removeDeadConnection(this);
			}
			TcpSender sender = getSender();
			if (sender != null && !(sender instanceof TcpConnectionInterceptorSupport)) {
				this.interceptedSenders.forEach(snder -> snder.removeDeadConnection(connection));
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public long incrementAndGetConnectionSequence() {
		return this.theConnection.incrementAndGetConnectionSequence();
	}

	@Override
	@Nullable
	public TcpSender getSender() {
		return this.interceptedSenders != null && this.interceptedSenders.size() > 0
				? this.interceptedSenders.get(0)
				: null;
	}

	@Override
	public List<TcpSender> getSenders() {
		return this.interceptedSenders == null
				? super.getSenders()
				: Collections.unmodifiableList(this.interceptedSenders);
	}

	protected boolean hasRealSender() {
		return this.realSender;
	}

}
