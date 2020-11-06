/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.event.inbound;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.integration.endpoint.ExpressionMessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An inbound Channel Adapter that implements {@link GenericApplicationListener} and
 * passes Spring {@link ApplicationEvent ApplicationEvents} within messages.
 * If a {@link #setPayloadExpression payloadExpression} is provided, it will be evaluated against
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
		implements GenericApplicationListener {

	private volatile Set<ResolvableType> eventTypes;

	private ApplicationEventMulticaster applicationEventMulticaster;

	private volatile long stoppedAt;

	public ApplicationEventListeningMessageProducer() {
		setPhase(Integer.MAX_VALUE / 2 - 1000);
	}

	/**
	 * Set the list of event types (classes that extend ApplicationEvent) that
	 * this adapter should send to the message channel. By default, all event
	 * types will be sent.
	 * In addition, this method re-registers the current instance as a {@link GenericApplicationListener}
	 * with the {@link ApplicationEventMulticaster} which clears the listener cache. The cache will be
	 * refreshed on the next appropriate {@link ApplicationEvent}.
	 * @param eventTypes The event types.
	 * @see ApplicationEventMulticaster#addApplicationListener
	 * @see #supportsEventType
	 */
	public final void setEventTypes(Class<?>... eventTypes) {
		Assert.notNull(eventTypes, "'eventTypes' must not be null");
		Set<ResolvableType> eventSet = new HashSet<>(eventTypes.length);
		for (Class<?> eventType : eventTypes) {
			if (eventType != null) {
				eventSet.add(ResolvableType.forClass(eventType));
			}
		}
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
		this.applicationEventMulticaster = getBeanFactory()
				.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
						ApplicationEventMulticaster.class);
		Assert.notNull(this.applicationEventMulticaster,
				"To use ApplicationListeners the 'applicationEventMulticaster' " +
						"bean must be supplied within ApplicationContext.");
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (isActive() || ((event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent)
				&& stoppedRecently())) {

			Object source = event.getSource();
			if (source instanceof Message<?>) {
				sendMessage((Message<?>) source);
			}
			else {
				Message<?> message;
				Object result = extractObjectToSend(event);
				if (result instanceof Message) {
					message = (Message<?>) result;
				}
				else {
					message = getMessageBuilderFactory().withPayload(result).build();
				}
				sendMessage(message);
			}
		}
	}

	private Object extractObjectToSend(Object root) {
		if (root instanceof PayloadApplicationEvent) {
			return ((PayloadApplicationEvent<?>) root).getPayload();
		}
		return evaluatePayloadExpression(root);
	}

	private boolean stoppedRecently() {
		return this.stoppedAt > System.currentTimeMillis() - 5000;
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		if (this.eventTypes == null) {
			return true;
		}

		for (ResolvableType type : this.eventTypes) {
			if (type.isAssignableFrom(eventType)) {
				return true;
			}
		}



		if (eventType.getRawClass() != null
				&& PayloadApplicationEvent.class.isAssignableFrom(eventType.getRawClass())) {
			if (eventType.hasUnresolvableGenerics()) {
				return true;
			}

			ResolvableType payloadType = eventType.as(PayloadApplicationEvent.class).getGeneric();
			for (ResolvableType type : this.eventTypes) {
				if (type.isAssignableFrom(payloadType)) {
					return true;
				}
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
	protected void doStop() {
		this.stoppedAt = System.currentTimeMillis();
		super.doStop();
	}

}

