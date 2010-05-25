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

package org.springframework.integration.endpoint;

import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;

/**
 * Message Endpoint that connects any {@link MessageHandler} implementation
 * to a {@link PollableChannel}.
 * 
 * @author Mark Fisher
 */
public class PollingConsumer extends AbstractPollingEndpoint {

	private final PollableChannel inputChannel;

	private final MessageHandler handler;

	private volatile MessageHandler handlerInvocationChain;

	private volatile long receiveTimeout = 1000;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public PollingConsumer(PollableChannel inputChannel, MessageHandler handler) {
		Assert.notNull(inputChannel, "inputChannel must not be null");
		Assert.notNull(handler, "handler must not be null");
		this.inputChannel = inputChannel;
		this.handler = handler;
	}


	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (!this.initialized) {
				this.handlerInvocationChain = new HandlerInvocationChain(this.handler, this.getComponentName());
			}
			this.initialized = true;
		}
		super.onInit();
	}

	@Override
	protected boolean doPoll() {
		Message<?> message = (this.receiveTimeout >= 0)
				? this.inputChannel.receive(this.receiveTimeout)
				: this.inputChannel.receive();
		if (message == null) {
			return false;
		}
		this.handlerInvocationChain.handleMessage(message);
		return true;
	}

}
