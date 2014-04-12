/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.ApplicationListener;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link MessageProducer} that produces Messages with @link {@link TcpConnectionEvent}
 * payloads.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TcpConnectionEventListeningMessageProducer extends MessageProducerSupport
	implements ApplicationListener<TcpConnectionEvent> {

	private volatile Set<Class<? extends TcpConnectionEvent>> eventTypes =
			new HashSet<Class<? extends TcpConnectionEvent>>();

	/**
	 * Set the list of event types (classes that extend TcpConnectionEvent) that
	 * this adapter should send to the message channel. By default, all event
	 * types will be sent.
	 *
	 * @param eventTypes The event types.
	 */
	public void setEventTypes(Class<? extends TcpConnectionEvent>[] eventTypes) {
		Assert.notEmpty(eventTypes, "at least one event type is required");
		Set<Class<? extends TcpConnectionEvent>> eventTypeSet = new HashSet<Class<? extends TcpConnectionEvent>>();
		eventTypeSet.addAll(Arrays.asList(eventTypes));
		this.eventTypes = eventTypeSet;
	}

	@Override
	public String getComponentType() {
		return "ip:tcp-connection-event-inbound-channel-adapter";
	}

	@Override
	public void onApplicationEvent(TcpConnectionEvent event) {
		if (this.isRunning()) {
			if (CollectionUtils.isEmpty(this.eventTypes)) {
				this.sendMessage(messageFromEvent(event));
			}
			else {
				for (Class<? extends TcpConnectionEvent> eventType : this.eventTypes) {
					if (eventType.isAssignableFrom(event.getClass())) {
						this.sendMessage(messageFromEvent(event));
						break;
					}
				}
			}
		}
	}

	protected Message<TcpConnectionEvent> messageFromEvent(TcpConnectionEvent event) {
		return this.getMessageBuilderFactory().withPayload(event).build();
	}

}
