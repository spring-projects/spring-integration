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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.stomp.event.StompExceptionEvent;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * @author Artem Bilan
 * @since 4.2
 */
public abstract class AbstractStompSessionManager implements StompSessionManager, ApplicationEventPublisherAware,
		DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private final CompositeStompSessionHandler compositeStompSessionHandler = new CompositeStompSessionHandler();

	private ApplicationEventPublisher applicationEventPublisher;

	private volatile StompHeaders connectHeaders;

	private volatile ListenableFuture<StompSession> stompSessionListenableFuture;

	private volatile StompSession stompSession;

	private volatile boolean autoReceipt;

	private volatile long destroyTimeout = 3000L;

	public void setAutoReceipt(boolean autoReceipt) {
		this.autoReceipt = autoReceipt;
	}

	@Override
	public boolean isAutoReceiptEnabled() {
		return this.autoReceipt;
	}

	public void setDestroyTimeout(long destroyTimeout) {
		this.destroyTimeout = destroyTimeout;
	}

	public void setConnectHeaders(StompHeaders connectHeaders) {
		this.connectHeaders = connectHeaders;
	}

	protected StompHeaders getConnectHeaders() {
		return connectHeaders;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public synchronized void connect(StompSessionHandler handler) {
		this.compositeStompSessionHandler.addHandler(handler);
		if (this.stompSessionListenableFuture == null) {
			this.stompSessionListenableFuture = doConnect(this.compositeStompSessionHandler);
			this.stompSessionListenableFuture.addCallback(new ListenableFutureCallback<StompSession>() {

				@Override
				public void onFailure(Throwable e) {
					logger.error("STOMP connect error.", e);
					if (applicationEventPublisher != null) {
						applicationEventPublisher.publishEvent(
								new StompExceptionEvent(AbstractStompSessionManager.this, e));
					}
					AbstractStompSessionManager.this.stompSession = null;
					AbstractStompSessionManager.this.stompSessionListenableFuture = null;
				}

				@Override
				public void onSuccess(StompSession stompSession) {
					AbstractStompSessionManager.this.stompSession = stompSession;
					AbstractStompSessionManager.this.stompSession.setAutoReceipt(autoReceipt);
				}

			});
		}
	}

	@Override
	public void disconnect(StompSessionHandler handler) {
		this.compositeStompSessionHandler.removeHandler(handler);
	}

	@Override
	public void destroy() throws Exception {
		if (this.stompSession != null) {
			this.stompSession.disconnect();
		}
		else if (this.stompSessionListenableFuture != null) {
			if (!this.stompSessionListenableFuture.isDone()) {
				this.stompSessionListenableFuture.addCallback(new ListenableFutureCallback<StompSession>() {
					@Override
					public void onFailure(Throwable ex) {

					}

					@Override
					public void onSuccess(StompSession session) {
						session.disconnect();
					}
				});
			}
			else {
				StompSession stompSession =
						this.stompSessionListenableFuture.get(this.destroyTimeout, TimeUnit.MILLISECONDS);
				if (stompSession != null) {
					stompSession.disconnect();
				}
			}
		}
	}

	protected abstract ListenableFuture<StompSession> doConnect(StompSessionHandler handler);


	private static class CompositeStompSessionHandler extends StompSessionHandlerAdapter {

		private final List<StompSessionHandler> delegates = new ArrayList<StompSessionHandler>();

		private volatile StompSession session;

		private volatile StompHeaders connectedHeaders;

		void addHandler(StompSessionHandler delegate) {
			if (this.session != null) {
				delegate.afterConnected(this.session, this.connectedHeaders);
			}
			this.delegates.add(delegate);
		}

		void removeHandler(StompSessionHandler delegate) {
			this.delegates.remove(delegate);
		}

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			this.session = session;
			this.connectedHeaders = connectedHeaders;
			for (StompSessionHandler delegate : this.delegates) {
				delegate.afterConnected(session, connectedHeaders);
			}
		}

		@Override
		public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
				Throwable exception) {
			for (StompSessionHandler delegate : this.delegates) {
				delegate.handleException(session, command, headers, payload, exception);
			}
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			for (StompSessionHandler delegate : this.delegates) {
				delegate.handleTransportError(session, exception);
			}
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			for (StompSessionHandler delegate : this.delegates) {
				delegate.handleFrame(headers, payload);
			}
		}

	}

}
