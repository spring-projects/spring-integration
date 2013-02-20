/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.ExpressionMessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * An inbound Channel Adapter that implements {@link ApplicationListener} and
 * passes Spring {@link ApplicationEvent ApplicationEvents} within messages.
 * If a {@link #setPayloadExpression(String) payloadExpression} is provided, it will be evaluated against
 * the ApplicationEvent instance to create the Message payload. Otherwise, the event itself will be the payload.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @see ApplicationEventMulticaster
 * @see ExpressionMessageProducerSupport
 */
public class ApplicationEventListeningMessageProducer extends ExpressionMessageProducerSupport implements SmartApplicationListener {

	private final Lock readEventTypesLock;
	private final Lock writeEventTypesLock;

	{
		ReadWriteLock lock = new ReentrantReadWriteLock();
		readEventTypesLock = lock.readLock();
		writeEventTypesLock = lock.writeLock();
	}


	private ApplicationEventMulticaster applicationEventMulticaster;

	private volatile Class<? extends ApplicationEvent>[] eventTypes;

	/**
	 * Set the list of event types (classes that extend ApplicationEvent) that
	 * this adapter should send to the message channel. By default, all event
	 * types will be sent.
	 * In additional this method re-register current instance as a {@link ApplicationListener}
	 * in the {@link ApplicationEventMulticaster} to clear listeners cache and get a fresh cache entry
	 * on next appropriate {@link ApplicationEvent}.
	 *
	 * @see ApplicationEventMulticaster#addApplicationListener
	 * @see #supportsEventType
	 */
	public void setEventTypes(Class<? extends ApplicationEvent>... eventTypes) {
		this.writeEventTypesLock.lock();
		try {
			this.eventTypes = eventTypes;
			if (this.applicationEventMulticaster != null) {
				this.applicationEventMulticaster.addApplicationListener(this);
			}
		}
		finally {
			this.writeEventTypesLock.unlock();
		}

	}

	public String getComponentType() {
		return "event:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.applicationEventMulticaster = this.getBeanFactory()
				.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
		Assert.notNull(this.applicationEventMulticaster,
				"To use ApplicationListeners the 'applicationEventMulticaster' bean must be supplied within ApplicationContext.");
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationContextEvent || this.isRunning()) {

			this.readEventTypesLock.lock();
			try {
				if(!this.supportsEventType(event.getClass())) {
					return;
				}
			}
			finally {
				this.readEventTypesLock.unlock();
			}

			if (event.getSource() instanceof Message<?>) {
				this.sendMessage((Message<?>) event.getSource());
			}
			else {
				Object payload = this.evaluatePayloadExpression(event);
				this.sendMessage(MessageBuilder.withPayload(payload).build());
			}
		}
	}

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

	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}

