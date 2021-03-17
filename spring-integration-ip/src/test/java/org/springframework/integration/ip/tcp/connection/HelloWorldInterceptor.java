/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class HelloWorldInterceptor extends TcpConnectionInterceptorSupport {

	private volatile boolean negotiated;

	private final Semaphore negotiationSemaphore = new Semaphore(0);

	private volatile long timeout = 10000;

	private volatile String hello = "Hello";

	private volatile String world = "world!";

	private volatile boolean closeReceived;

	private volatile boolean pendingSend;

	public HelloWorldInterceptor() {
	}

	public HelloWorldInterceptor(String hello, String world, ApplicationEventPublisher applicationEventPublisher) {
		super(applicationEventPublisher);
		this.hello = hello;
		this.world = world;
	}

	@Override
	public boolean onMessage(Message<?> message) {
		if (!this.negotiated) {
			synchronized (this) {
				if (!this.negotiated) {
					Object payload = message.getPayload();
					logger.debug(this.toString() + " received " + payload);
					if (this.isServer()) {
						if (payload.equals(hello)) {
							try {
								logger.debug(this.toString() + " sending " + this.world);
								super.send(MessageBuilder.withPayload(world).build());
								this.negotiated = true;
								return true;
							}
							catch (Exception e) {
								throw new MessagingException("Negotiation error", e);
							}
						}
						else {
							throw new MessagingException("Negotiation error, expected '" + hello +
									"' received '" + payload + "'");
						}
					}
					else {
						if (payload.equals(world)) {
							this.negotiated = true;
							this.negotiationSemaphore.release();
						}
						else {
							throw new MessagingException("Negotiation error - expected '" + world +
									"' received " + payload);
						}
						return true;
					}
				}
			}
		}
		try {
			return super.onMessage(message);
		}
		finally {
			// on the server side, we don't want to close if we are expecting a response
			if (!(this.isServer() && this.hasRealSender()) && !this.pendingSend) {
				this.checkDeferredClose();
			}
		}
	}

	@Override
	public void send(Message<?> message) {
		this.pendingSend = true;
		try {
			if (!this.negotiated) {
				if (!this.isServer()) {
					logger.debug(this.toString() + " Sending " + hello);
					super.send(MessageBuilder.withPayload(hello).build());
					try {
						this.negotiationSemaphore.tryAcquire(this.timeout, TimeUnit.MILLISECONDS);
					}
					catch (@SuppressWarnings("unused") InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					if (!this.negotiated) {
						throw new MessagingException("Negotiation error");
					}
				}
			}
			super.send(message);
		}
		finally {
			this.pendingSend = false;
			this.checkDeferredClose();
		}
	}

	/**
	 * Defer the close until we've actually sent the data after negotiation
	 */
	@Override
	public void close() {
		if (this.negotiated && !this.pendingSend) {
			super.close();
			return;
		}
		closeReceived = true;
		logger.debug("Deferring close");
	}

	/**
	 * Execute the close, if deferred
	 */
	private void checkDeferredClose() {
		if (this.closeReceived) {
			logger.debug("Executing deferred close");
			this.close();
		}
	}

	@Override
	public String toString() {
		return "HelloWorldInterceptor [negotiated=" + negotiated + ", hello=" + hello + ", world=" + world
				+ ", closeReceived=" + closeReceived + ", pendingSend=" + pendingSend + "]";
	}

}
