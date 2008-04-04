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
import java.util.Collection;
import java.util.List;

import org.springframework.integration.adapter.PollableSource;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.CollectionUtils;

/**
 * A channel that invokes any subscribed {@link MessageHandler handler(s)} in a
 * sender's thread. If a {@link PollableSource} is provided, then that source
 * will likewise be polled within a receiver's thread. If no source is provided,
 * then receive() will always return null.
 * 
 * @author Mark Fisher
 */
public class SynchronousChannel extends AbstractMessageChannel {

	private final MessageDistributor distributor;

	private volatile PollableSource<?> source;


	public SynchronousChannel() {
		this(null);
	}

	public SynchronousChannel(DispatcherPolicy dispatcherPolicy) {
		super(dispatcherPolicy != null ? dispatcherPolicy : new DispatcherPolicy());
		this.distributor = new DefaultMessageDistributor(this.getDispatcherPolicy());
	}


	public void setSource(PollableSource<?> source) {
		this.source = source;
	}

	public void addHandler(MessageHandler handler) {
		this.distributor.addHandler(handler);
	}

	@Override
	protected Message<?> doReceive(long timeout) {
		if (this.source != null) {
			Collection<?> results = this.source.poll(1);
			if (!CollectionUtils.isEmpty(results)) {
				Object result = results.iterator().next();
				return (result instanceof Message<?>) ? (Message<?>) result :
						new SimplePayloadMessageMapper<Object>().toMessage(result);
			}
		}
		return null;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		return this.distributor.distribute(message);
	}

	public List<Message<?>> clear() {
		return new ArrayList<Message<?>>();
	}

	public List<Message<?>> purge(MessageSelector selector) {
		return new ArrayList<Message<?>>();
	}

}
