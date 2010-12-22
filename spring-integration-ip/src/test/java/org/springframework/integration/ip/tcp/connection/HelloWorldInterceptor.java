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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class HelloWorldInterceptor extends AbstractTcpConnectionInterceptor {

	Log logger = LogFactory.getLog(this.getClass());
	
	private boolean negotiated;
	
	private Semaphore negotiationSemaphore = new Semaphore(0);
	
	private long timeout = 10000;
	
	private String hello = "Hello";

	private String world = "world!";
	
	private boolean closeReceived;

	public HelloWorldInterceptor() { 
	}
	
	/**
	 * @param hello
	 * @param world
	 */
	public HelloWorldInterceptor(String hello, String world) {
		super();
		this.hello = hello;
		this.world = world;
	}

	@Override
	public boolean onMessage(Message<?> message) {
		if (!this.negotiated) {
			Object payload = message.getPayload();
			if (this.isServer()) {
				if (payload.equals(hello)) {
					try {
						logger.debug("sending " + this.world);
						super.send(MessageBuilder.withPayload(world).build());
						this.negotiated = true;
						return true;
					} catch (Exception e) {
						throw new MessagingException("Negotiation error", e);
					}
				} else {
					throw new MessagingException("Negotiation error, expected '" + hello + 
							     "' received '" + payload + "'");
				}
			} else {
				logger.debug("received " + payload);
				if (payload.equals(world)) {
					this.negotiated = true;
					this.negotiationSemaphore.release();
				} else {
					throw new MessagingException("Negotiation error - expected '" + world + 
								"' received " + payload);
				}
				return true;
			}
		}
		try {
			return super.onMessage(message);
		} finally {
			// on the server side, we don't want to close if we are expecting a response 
			if (!(this.isServer() && this.hasRealSender())) {
				this.checkDeferredClose();
			}
		}
	}

	@Override
	public void send(Message<?> message) throws Exception {
		try {
			if (!this.negotiated) {
				if (!this.isServer()) {
					logger.debug("Sending " + hello);
					super.send(MessageBuilder.withPayload(hello).build());
					this.negotiationSemaphore.tryAcquire(this.timeout, TimeUnit.MILLISECONDS);
					if (!this.negotiated) {
						throw new MessagingException("Negotiation error");
					}
				}
			}
			super.send(message);
		} finally {
			this.checkDeferredClose();
		}
	}

	/**
	 * Defer the close until we've actually sent the data after negotiation
	 */
	@Override
	public void close() {
		if (this.negotiated) {
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

	

}
