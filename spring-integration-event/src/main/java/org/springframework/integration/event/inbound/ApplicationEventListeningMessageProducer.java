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

package org.springframework.integration.event.inbound;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.endpoint.ExpressionMessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An inbound Channel Adapter that implements {@link ApplicationListener} and
 * passes Spring {@link ApplicationEvent ApplicationEvents} within messages.
 * If a {@link #setPayloadExpression(String) payloadExpression} is provided, it will be evaluated against
 * the ApplicationEvent instance to create the Message payload. Otherwise, the event itself will be the payload.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @see ApplicationEventMulticaster
 * @see ExpressionMessageProducerSupport
 */
public class ApplicationEventListeningMessageProducer extends ExpressionMessageProducerSupport
		implements SmartApplicationListener {

	private volatile Set<Class<? extends ApplicationEvent>> eventTypes;

	private ApplicationEventMulticaster applicationEventMulticaster;

	private volatile boolean active;

	private volatile long stoppedAt;

	/**
	 * Set the list of event types (classes that extend ApplicationEvent) that
	 * this adapter should send to the message channel. By default, all event
	 * types will be sent.
	 * In addition, this method re-registers the current instance as a {@link ApplicationListener}
	 * with the {@link ApplicationEventMulticaster} which clears the listener cache. The cache will be
	 * refreshed on the next appropriate {@link ApplicationEvent}.
	 *
	 * @param eventTypes The event types.
	 *
	 * @see ApplicationEventMulticaster#addApplicationListener
	 * @see #supportsEventType
	 */
	public void setEventTypes(Class<? extends ApplicationEvent>... eventTypes) {
		Set<Class<? extends ApplicationEvent>> eventSet = new HashSet<Class<? extends ApplicationEvent>>(
				Arrays.asList(eventTypes));
		eventSet.remove(null);
		this.eventTypes = (eventSet.size() > 0 ? eventSet : null);

		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(this);
		}
	}

	@Override
	public String getComponentType() {
		return "event:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.applicationEventMulticaster = this.getBeanFactory()
				.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
						ApplicationEventMulticaster.class);
		Assert.notNull(this.applicationEventMulticaster,
				"To use ApplicationListeners the 'applicationEventMulticaster' " +
						"bean must be supplied within ApplicationContext.");
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (this.active || ((event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent)
								&& this.stoppedRecently())) {
			if (event.getSource() instanceof Message<?>) {
				this.sendMessage((Message<?>) event.getSource());
			}
			else {
				Object payload = this.evaluatePayloadExpression(event);
				this.sendMessage(this.getMessageBuilderFactory().withPayload(payload).build());
			}
		}
	}

	private boolean stoppedRecently() {
		return this.stoppedAt > System.currentTimeMillis() - 5000;
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		if (this.eventTypes == null) {
			return true;
		}
		for (Class<? extends ApplicationEvent> type : this.eventTypes) {
			if (type.isAssignableFrom(eventType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	protected void doStart() {
		this.active = true;
	}

	@Override
	protected void doStop() {
		this.stoppedAt = System.currentTimeMillis();
		this.active = false;
	}

}

