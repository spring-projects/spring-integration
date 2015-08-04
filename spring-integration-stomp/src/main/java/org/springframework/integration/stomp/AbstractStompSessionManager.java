/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.integration.stomp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.stomp.event.StompExceptionEvent;
import org.springframework.messaging.simp.stomp.StompClientSupport;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Base {@link StompSessionManager} implementation to manage a single {@link StompSession}
 * over its {@link ListenableFuture} from the target implementation of this class.
 * <p>
 * The connection to the {@link StompSession} is made during {@link #afterPropertiesSet()}.
 * <p>
 * The {@link #destroy()} lifecycle method manages {@link StompSession#disconnect()}.
 * <p>
 * The {@link #connect(StompSessionHandler)} and {@link #disconnect(StompSessionHandler)} method
 * implementations populate/remove the provided {@link StompSessionHandler} to/from an internal
 * {@link AbstractStompSessionManager.CompositeStompSessionHandler}, which delegates all operations
 * to the provided {@link StompSessionHandler}s.
 * This {@link AbstractStompSessionManager.CompositeStompSessionHandler} is used for the
 * {@link StompSession} connection.
 *
 * @author Artem Bilan
 * @since 4.2
 */
public abstract class AbstractStompSessionManager implements StompSessionManager, ApplicationEventPublisherAware,
		InitializingBean, DisposableBean, BeanNameAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final CompositeStompSessionHandler compositeStompSessionHandler = new CompositeStompSessionHandler();

	protected final StompClientSupport stompClient;

	private ApplicationEventPublisher applicationEventPublisher;

	private volatile StompHeaders connectHeaders;

	private volatile ListenableFuture<StompSession> stompSessionListenableFuture;

	private volatile boolean autoReceipt;

	private volatile boolean connected;

	private String name;

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

	@Override
	public void afterPropertiesSet() throws Exception {
		this.stompSessionListenableFuture = doConnect(this.compositeStompSessionHandler);
		this.stompSessionListenableFuture.addCallback(new ListenableFutureCallback<StompSession>() {

			@Override
			public void onFailure(Throwable e) {
				logger.error("STOMP connect error.", e);
				if (applicationEventPublisher != null) {
					applicationEventPublisher.publishEvent(
							new StompExceptionEvent(AbstractStompSessionManager.this, e));
				}
			}

			@Override
			public void onSuccess(StompSession stompSession) {
				stompSession.setAutoReceipt(autoReceipt);
				AbstractStompSessionManager.this.connected = true;
			}

		});
	}

	@Override
	public void destroy() throws Exception {
		this.stompSessionListenableFuture.addCallback(new ListenableFutureCallback<StompSession>() {

			@Override
			public void onFailure(Throwable ex) {
				AbstractStompSessionManager.this.connected = false;
			}

			@Override
			public void onSuccess(StompSession session) {
				session.disconnect();
				AbstractStompSessionManager.this.connected = false;
			}

		});
	}

	@Override
	public void connect(StompSessionHandler handler) {
		this.compositeStompSessionHandler.addHandler(handler);
	}

	@Override
	public void disconnect(StompSessionHandler handler) {
		this.compositeStompSessionHandler.removeHandler(handler);
	}

	protected StompHeaders getConnectHeaders() {
		return connectHeaders;
	}

	@Override
	public String toString() {
		return "StompSessionManager{" +
				"connected=" + connected +
				", name='" + name + '\'' +
				'}';
	}

	protected abstract ListenableFuture<StompSession> doConnect(StompSessionHandler handler);


	private static class CompositeStompSessionHandler extends StompSessionHandlerAdapter {

		private final List<StompSessionHandler> delegates =
				Collections.synchronizedList(new ArrayList<StompSessionHandler>());

		private volatile StompSession session;

		private volatile StompHeaders connectedHeaders;

		void addHandler(StompSessionHandler delegate) {
			if (this.session != null) {
				delegate.afterConnected(this.session, this.connectedHeaders);
			}
			synchronized (this.delegates) {
				this.delegates.add(delegate);
			}
		}

		void removeHandler(StompSessionHandler delegate) {
			synchronized (this.delegates) {
				this.delegates.remove(delegate);
			}
		}

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			this.session = session;
			this.connectedHeaders = connectedHeaders;
			synchronized (this.delegates) {
				for (StompSessionHandler delegate : this.delegates) {
					delegate.afterConnected(session, connectedHeaders);
				}
			}
		}

		@Override
		public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
				Throwable exception) {
			synchronized (this.delegates) {
				for (StompSessionHandler delegate : this.delegates) {
					delegate.handleException(session, command, headers, payload, exception);
				}
			}
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			synchronized (this.delegates) {
				for (StompSessionHandler delegate : this.delegates) {
					delegate.handleTransportError(session, exception);
				}
			}
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			synchronized (this.delegates) {
				for (StompSessionHandler delegate : this.delegates) {
					delegate.handleFrame(headers, payload);
				}
			}
		}

	}

}
