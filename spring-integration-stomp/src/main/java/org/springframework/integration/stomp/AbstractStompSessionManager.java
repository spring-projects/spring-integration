/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.stomp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.stomp.event.StompConnectionFailedEvent;
import org.springframework.integration.stomp.event.StompSessionConnectedEvent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompClientSupport;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Base {@link StompSessionManager} implementation to manage a single {@link StompSession}
 * over its {@link ListenableFuture} from the target implementation of this class.
 * <p>
 * The connection to the {@link StompSession} is made during {@link #start()}.
 * <p>
 * The {@link #stop()} lifecycle method manages {@link StompSession#disconnect()}.
 * <p>
 * The {@link #connect(StompSessionHandler)} and {@link #disconnect(StompSessionHandler)} method
 * implementations populate/remove the provided {@link StompSessionHandler} to/from an internal
 * {@link AbstractStompSessionManager.CompositeStompSessionHandler}, which delegates all operations
 * to the provided {@link StompSessionHandler}s.
 * This {@link AbstractStompSessionManager.CompositeStompSessionHandler} is used for the
 * {@link StompSession} connection.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Christian Tzolov
 *
 * @since 4.2
 */
public abstract class AbstractStompSessionManager implements StompSessionManager, ApplicationEventPublisherAware,
		SmartLifecycle, DisposableBean, BeanNameAware {

	private static final long DEFAULT_RECOVERY_INTERVAL = 10000;

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	protected final StompClientSupport stompClient; // NOSONAR final

	private final CompositeStompSessionHandler compositeStompSessionHandler = new CompositeStompSessionHandler();

	private final Lock lifecycleMonitor = new ReentrantLock();

	private final Lock lock = new ReentrantLock();

	private final AtomicInteger epoch = new AtomicInteger();

	private boolean autoStartup = false;

	private boolean running = false;

	private int phase = Integer.MAX_VALUE / 2;

	private ApplicationEventPublisher applicationEventPublisher;

	private StompHeaders connectHeaders;

	private boolean autoReceipt;

	private long recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

	private String name;

	private volatile boolean connecting;

	private volatile boolean connected;

	private volatile CompletableFuture<StompSession> stompSessionFuture;

	private volatile ScheduledFuture<?> reconnectFuture;

	public AbstractStompSessionManager(StompClientSupport stompClient) {
		Assert.notNull(stompClient, "'stompClient' is required.");
		this.stompClient = stompClient;
	}

	public void setConnectHeaders(StompHeaders connectHeaders) {
		this.connectHeaders = connectHeaders;
	}

	public void setAutoReceipt(boolean autoReceipt) {
		this.autoReceipt = autoReceipt;
	}

	@Override
	public boolean isAutoReceiptEnabled() {
		return this.autoReceipt;
	}

	@Override
	public boolean isConnected() {
		return this.connected;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void setBeanName(String name) {
		this.name = name;
	}

	/**
	 * Specify a reconnect interval in milliseconds in case of lost connection.
	 * @param recoveryInterval the reconnect interval in milliseconds in case of lost connection.
	 * @since 4.2.2
	 */
	public void setRecoveryInterval(int recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public long getRecoveryInterval() {
		return this.recoveryInterval;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	private void connect() {
		this.lock.lock();
		try {
			if (this.connecting || this.connected) {
				this.logger.debug("Aborting connect; another thread is connecting.");
				return;
			}
			final int currentEpoch = this.epoch.get();
			this.connecting = true;
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Connecting " + this);
			}
			try {
				this.stompSessionFuture = doConnect(this.compositeStompSessionHandler);
			}
			catch (Exception e) {
				if (currentEpoch == this.epoch.get()) {
					scheduleReconnect(e);
				}
				else {
					this.logger.error("STOMP doConnect() error for " + this, e);
				}
				return;
			}
			CountDownLatch connectLatch = addStompSessionCallback(currentEpoch);

			try {
				if (!connectLatch.await(30, TimeUnit.SECONDS)) { // NOSONAR magic number
					this.logger.error("No response to connection attempt");
					if (currentEpoch == this.epoch.get()) {
						scheduleReconnect(null);
					}
				}
			}
			catch (InterruptedException e1) {
				this.logger.error("Interrupted while waiting for connection attempt");
				Thread.currentThread().interrupt();
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	private CountDownLatch addStompSessionCallback(int currentEpoch) {
		CountDownLatch connectLatch = new CountDownLatch(1);
		this.stompSessionFuture.whenComplete((stompSession, throwable) -> {
					if (throwable == null) {
						AbstractStompSessionManager.this.logger.debug("onSuccess");
						AbstractStompSessionManager.this.connected = true;
						AbstractStompSessionManager.this.connecting = false;
						if (stompSession != null) {
							stompSession.setAutoReceipt(isAutoReceiptEnabled());
						}
						if (AbstractStompSessionManager.this.applicationEventPublisher != null) {
							AbstractStompSessionManager.this.applicationEventPublisher.publishEvent(
									new StompSessionConnectedEvent(this));
						}
						AbstractStompSessionManager.this.reconnectFuture = null;
						connectLatch.countDown();
					}
					else {
						AbstractStompSessionManager.this.logger.debug("onFailure", throwable);
						connectLatch.countDown();
						if (currentEpoch == AbstractStompSessionManager.this.epoch.get()) {
							scheduleReconnect(throwable);
						}
					}
				}
		);
		return connectLatch;
	}

	private void scheduleReconnect(Throwable e) {
		this.epoch.incrementAndGet();
		this.connecting = false;
		this.connected = false;
		if (e != null) {
			this.logger.error("STOMP connect error for " + this, e);
		}
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(
					new StompConnectionFailedEvent(this, e));
		}
		// cancel() after the publish in case we are on that thread; a send to a QueueChannel would fail.
		if (this.reconnectFuture != null) {
			this.reconnectFuture.cancel(true);
			this.reconnectFuture = null;
		}

		TaskScheduler taskScheduler = this.stompClient.getTaskScheduler();
		if (taskScheduler != null) {
			this.reconnectFuture =
					taskScheduler.schedule(this::connect, Instant.now().plusMillis(this.recoveryInterval));
		}
		else {
			this.logger.info("For automatic reconnection the stompClient should be configured with a TaskScheduler.");
		}
	}

	@Override
	public void destroy() {
		if (this.stompSessionFuture != null) {
			if (this.reconnectFuture != null) {
				this.reconnectFuture.cancel(false);
				this.reconnectFuture = null;
			}
			this.stompSessionFuture.whenComplete(new BiConsumer<StompSession, Throwable>() {

				@Override
				public void accept(StompSession session, Throwable throwable) {
					if (session != null) {
						session.disconnect();
					}
					AbstractStompSessionManager.this.connected = false;
				}
			});
			this.stompSessionFuture = null;
		}
	}

	@Override
	public void start() {
		this.lifecycleMonitor.lock();
		try {
			if (!isRunning()) {
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Starting " + this);
				}
				connect();
				this.running = true;
			}
		}
		finally {
			this.lifecycleMonitor.unlock();
		}
	}

	@Override
	public void stop() {
		this.lifecycleMonitor.lock();
		try {
			if (isRunning()) {
				this.running = false;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Stopping " + this);
				}
				destroy();
			}
		}
		finally {
			this.lifecycleMonitor.unlock();
		}
	}

	@Override
	public void connect(StompSessionHandler handler) {
		this.compositeStompSessionHandler.addHandler(handler);
		if (!isConnected() && !this.connecting) {
			if (this.reconnectFuture != null) {
				this.reconnectFuture.cancel(true);
				this.reconnectFuture = null;
			}
			connect();
		}
	}

	@Override
	public void disconnect(StompSessionHandler handler) {
		this.compositeStompSessionHandler.removeHandler(handler);
	}

	protected StompHeaders getConnectHeaders() {
		return this.connectHeaders;
	}

	@Override
	public String toString() {
		return ObjectUtils.identityToString(this) +
				" {connecting=" + this.connecting +
				", connected=" + this.connected +
				", name='" + this.name + '\'' +
				'}';
	}

	protected abstract CompletableFuture<StompSession> doConnect(StompSessionHandler handler);


	private class CompositeStompSessionHandler extends StompSessionHandlerAdapter {

		private final List<StompSessionHandler> delegates = Collections.synchronizedList(new ArrayList<>());

		private final Lock delegatesMonitor = new ReentrantLock();

		private volatile StompSession session;

		CompositeStompSessionHandler() {
		}

		void addHandler(StompSessionHandler delegate) {
			this.delegatesMonitor.lock();
			try {
				if (this.session != null) {
					delegate.afterConnected(this.session, getConnectHeaders());
				}
				this.delegates.add(delegate);
			}
			finally {
				this.delegatesMonitor.unlock();
			}
		}

		void removeHandler(StompSessionHandler delegate) {
			this.delegates.remove(delegate);
		}

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			this.delegatesMonitor.lock();
			try {
				this.session = session;
				for (StompSessionHandler delegate : this.delegates) {
					delegate.afterConnected(session, connectedHeaders);
				}
			}
			finally {
				this.delegatesMonitor.unlock();
			}
		}

		@Override
		public void handleException(StompSession session, @Nullable StompCommand command, StompHeaders headers,
				byte[] payload, Throwable exception) {

			this.delegatesMonitor.lock();
			try {
				for (StompSessionHandler delegate : this.delegates) {
					delegate.handleException(session, command, headers, payload, exception);
				}
			}
			finally {
				this.delegatesMonitor.unlock();
			}
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			AbstractStompSessionManager.this.logger.error("STOMP transport error for session: [" + session + "]",
					exception);
			this.session = null;
			scheduleReconnect(exception);
			this.delegatesMonitor.lock();
			try {
				for (StompSessionHandler delegate : this.delegates) {
					delegate.handleTransportError(session, exception);
				}
			}
			finally {
				this.delegatesMonitor.unlock();
			}
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			this.delegatesMonitor.lock();
			try {
				for (StompSessionHandler delegate : this.delegates) {
					delegate.handleFrame(headers, payload);
				}
			}
			finally {
				this.delegatesMonitor.unlock();
			}
		}

	}

}
