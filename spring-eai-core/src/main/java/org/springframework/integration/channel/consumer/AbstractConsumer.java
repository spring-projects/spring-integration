/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.channel.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.MessageSource;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * Base class for consumers defining common properties and behavior.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractConsumer implements Lifecycle {

	/**
	 * The default receive timeout: 1000 ms = 1 second.
	 */
	public static final long DEFAULT_RECEIVE_TIMEOUT = 1000;


	protected final Log logger = LogFactory.getLog(getClass());

	private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;

	private MessageSource source;

	private MessageEndpoint endpoint;

	private boolean active = false;

	private boolean running = false;

	private boolean autoStartup = true;

	protected final Object lifecycleMonitor = new Object();


	public AbstractConsumer(MessageSource source, MessageEndpoint endpoint) {
		Assert.notNull(source, "source must not be null");
		Assert.notNull(endpoint, "endpoint must not be null");
		this.source = source;
		this.endpoint = endpoint;
	}


	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public final boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	public final void start() {
		synchronized (this.lifecycleMonitor) {
			this.running = true;
			this.lifecycleMonitor.notifyAll();
		}
		this.doStart();
	}

	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.lifecycleMonitor.notifyAll();
		}
		this.doStop();
	}

	public final boolean isActive() {
		synchronized (this.lifecycleMonitor) {
			return this.active;
		}
	}

	public final void initialize() {
		synchronized (this.lifecycleMonitor) {
			this.active = true;
			this.lifecycleMonitor.notifyAll();
		}
		doInitialize();
		if (this.autoStartup) {
			synchronized (this.lifecycleMonitor) {
				this.start();
			}
		}
	}

	/**
	 * If the consumer is active but not yet running, then wait until it is running.
	 */
	protected void waitWhileNotRunning() {
		synchronized (this.lifecycleMonitor) {
			while (this.active && !this.running) {
				try {
					this.lifecycleMonitor.wait();
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	protected boolean receiveAndPassToEndpoint() {
		boolean messageReceived = false;
		Message message = null;
		if (this.receiveTimeout < 0) { // indefinite timeout
			message = this.source.receive();
		}
		else {
			message = this.source.receive(this.receiveTimeout);
		}
		if (message != null) {
			messageReceived = true;
			messageReceived(message);
			this.endpoint.messageReceived(message);
		}
		return messageReceived;
	}


	protected abstract void doInitialize();

	protected abstract void doStart();

	protected abstract void doStop();

	protected abstract void messageReceived(Message message);

}
