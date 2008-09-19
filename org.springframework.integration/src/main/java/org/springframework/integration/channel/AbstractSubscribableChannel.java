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

package org.springframework.integration.channel;

import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.Subscribable;
import org.springframework.util.Assert;

/**
 * Base implementation of {@link MessageChannel} that invokes the subscribed
 * {@link MessageConsumer consumer(s)} by delegating to a {@link MessageDispatcher}.
 * 
 * @author Mark Fisher
 */
public class AbstractSubscribableChannel<T extends MessageDispatcher> extends AbstractMessageChannel implements Subscribable {

	private final T dispatcher;


	public AbstractSubscribableChannel(T dispatcher) {
		Assert.notNull(dispatcher, "dispatcher must not be null");
		this.dispatcher = dispatcher;
	}


	protected T getDispatcher() {
		return this.dispatcher;
	}

	public boolean subscribe(MessageConsumer consumer) {
		return this.dispatcher.subscribe(consumer);
	}

	public boolean unsubscribe(MessageConsumer consumer) {
		return this.dispatcher.unsubscribe(consumer);
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		return this.dispatcher.dispatch(message);
	}

}
