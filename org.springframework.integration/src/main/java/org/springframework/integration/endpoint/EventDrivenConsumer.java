/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;

/**
 * Message Endpoint that connects any {@link MessageHandler} implementation
 * to a {@link SubscribableChannel}.
 * 
 * @author Mark Fisher
 */
public class EventDrivenConsumer extends AbstractEndpoint implements Lifecycle {

	private final SubscribableChannel inputChannel;

	private final MessageHandler handler;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public EventDrivenConsumer(SubscribableChannel inputChannel, MessageHandler handler) {
		Assert.notNull(inputChannel, "inputChannel must not be null");
		Assert.notNull(handler, "handler must not be null");
		this.inputChannel = inputChannel;
		this.handler = handler;
	}


	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				this.inputChannel.subscribe(this.handler);
				this.running = true;
			}
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				this.inputChannel.unsubscribe(this.handler);
				this.running = false;
			}
		}
	}

}
