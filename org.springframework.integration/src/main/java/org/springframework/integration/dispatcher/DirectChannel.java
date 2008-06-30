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

package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.Subscribable;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * A channel that invokes the subscribed {@link MessageHandler handler(s)} in a
 * sender's thread (returning after at most one handles the message). If a
 * {@link MessageSource} is provided, then that source will likewise be polled
 * within a receiver's thread.
 * 
 * @author Dave Syer
 * @author Mark Fisher
 */
public class DirectChannel extends AbstractMessageChannel implements Subscribable {

	private volatile MessageSource<?> source;

	private final SimpleDispatcher dispatcher;

	private final AtomicInteger handlerCount = new AtomicInteger();


	public DirectChannel() {
		this(null);
	}

	public DirectChannel(MessageSource<?> source) {
		super(defaultDispatcherPolicy());
		this.source = source;
		this.dispatcher = new SimpleDispatcher(this.getDispatcherPolicy());
	}


	public boolean subscribe(MessageTarget target) {
		boolean added = this.dispatcher.subscribe(target);
		if (added) {
			this.handlerCount.incrementAndGet();
		}
		return added;
	}

	public boolean unsubscribe(MessageTarget target) {
		boolean removed = this.dispatcher.unsubscribe(target);
		if (removed) {
			this.handlerCount.decrementAndGet();
		}
		return removed;
	}


	@Override
	protected Message<?> doReceive(long timeout) {
		if (this.source != null) {
			return this.source.receive();
		}
		return null;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (message != null && this.handlerCount.get() > 0) {
			return this.dispatcher.dispatch(message);
		}
		return false;
	}

	public List<Message<?>> clear() {
		return new ArrayList<Message<?>>();
	}

	public List<Message<?>> purge(MessageSelector selector) {
		return new ArrayList<Message<?>>();
	}


	private static DispatcherPolicy defaultDispatcherPolicy() {
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy(false);
		dispatcherPolicy.setMaxMessagesPerTask(1);
		dispatcherPolicy.setReceiveTimeout(0);
		dispatcherPolicy.setRejectionLimit(1);
		dispatcherPolicy.setRetryInterval(0);
		dispatcherPolicy.setShouldFailOnRejectionLimit(false);
		return dispatcherPolicy;
	}

}
