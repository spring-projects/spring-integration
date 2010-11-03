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

package org.springframework.integration.event.inbound;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * An inbound Channel Adapter that passes Spring {@link ApplicationEvent ApplicationEvents} within messages.
 * If a {@link #setPayloadExpression(String) payloadExpression} is provided, it will be evaluated against
 * the ApplicationEvent instance to create the Message payload. Otherwise, the event itself will be the payload.
 * 
 * @author Mark Fisher
 */
public class ApplicationEventListeningMessageProducer extends MessageProducerSupport implements ApplicationListener<ApplicationEvent> {

	private final Set<Class<? extends ApplicationEvent>> eventTypes = new CopyOnWriteArraySet<Class<? extends ApplicationEvent>>();

	private volatile Expression payloadExpression;

	private volatile boolean active;

	private final SpelExpressionParser parser = new SpelExpressionParser();


	/**
	 * Set the list of event types (classes that extend ApplicationEvent) that
	 * this adapter should send to the message channel. By default, all event
	 * types will be sent.
	 */
	@SuppressWarnings("unchecked")
	public void setEventTypes(Class<? extends ApplicationEvent>[] eventTypes) {
		Assert.notEmpty(eventTypes, "at least one event type is required");
		synchronized (this.eventTypes) {
			this.eventTypes.clear();
			this.eventTypes.addAll(CollectionUtils.arrayToList(eventTypes));
		}
	}

	/**
	 * Provide an expression to be evaluated against the received ApplicationEvent
	 * instance (the "root object") in order to create the Message payload. If none
	 * is provided, the ApplicationEvent itself will be used as the payload.
	 */
	public void setPayloadExpression(String payloadExpression) {
		if (payloadExpression == null) {
			this.payloadExpression = null;
		}
		else {
			this.payloadExpression = this.parser.parseExpression(payloadExpression);
		}
	}

	public String getComponentType() {
		return "event:inbound-channel-adapter";
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (this.active || event instanceof ApplicationContextEvent) {
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
	}

	@Override
	protected void doStart() {
		this.active = true;
	}

	@Override
	protected void doStop() {
		this.active = false;
	}

	private void sendEventAsMessage(ApplicationEvent event) {
		if (event.getSource() instanceof Message<?>) {
			this.sendMessage((Message<?>) event.getSource());
		}
		else {
			Object payload = (this.payloadExpression != null) ? this.payloadExpression.getValue(event) : event;
			this.sendMessage(MessageBuilder.withPayload(payload).build());
		}
	}

}
