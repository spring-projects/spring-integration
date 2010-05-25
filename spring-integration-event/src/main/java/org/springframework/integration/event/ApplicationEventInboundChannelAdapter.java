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

package org.springframework.integration.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * An inbound Channel Adapter that passes Spring
 * {@link ApplicationEvent ApplicationEvents} within messages.
 * 
 * @author Mark Fisher
 */
public class ApplicationEventInboundChannelAdapter extends MessageProducerSupport implements ApplicationListener<ApplicationEvent> {

	private final List<Class<? extends ApplicationEvent>> eventTypes = new CopyOnWriteArrayList<Class<? extends ApplicationEvent>>();


	/**
	 * Set the list of event types (classes that extend ApplicationEvent) that
	 * this adapter should send to the message channel. By default, all event
	 * types will be sent.
	 */
	public void setEventTypes(List<Class<? extends ApplicationEvent>> eventTypes) {
		Assert.notEmpty(eventTypes, "at least one event type is required");
		synchronized (this.eventTypes) {
			this.eventTypes.clear();
			this.eventTypes.addAll(eventTypes);
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (CollectionUtils.isEmpty(this.eventTypes)) {
			this.sendEventAsMessage(event);
			return;
		}
		for (Class<? extends ApplicationEvent> eventType : this.eventTypes) {
			if (eventType.isAssignableFrom(event.getClass())) {
				this.sendEventAsMessage(event);
				return;
			}
		}
	}

	private boolean sendEventAsMessage(ApplicationEvent event) {
		return this.sendMessage(MessageBuilder.withPayload(event).build());
	}

	@Override
	protected void doStart() {
	}

	@Override
	protected void doStop() {
	}

}
