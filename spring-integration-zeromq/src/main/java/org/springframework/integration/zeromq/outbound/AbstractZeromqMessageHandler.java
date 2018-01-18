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

package org.springframework.integration.zeromq.outbound;

import java.util.concurrent.atomic.AtomicBoolean;

import org.zeromq.SocketType;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract class for ZMQ outbound channel adapters.
 *
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
public abstract class AbstractZeromqMessageHandler extends AbstractMessageHandler implements Lifecycle {

	private static final MessageProcessor<String> DEFAULT_TOPIC_PROCESSOR =
			m -> (String) m.getHeaders().get("topic");

	private final AtomicBoolean running = new AtomicBoolean();

	private final String url;

	private final String clientId;

	private String topic;

	private MessageProcessor<String> topicProcessor = DEFAULT_TOPIC_PROCESSOR;

	private MessageConverter converter;

	private int clientType = SocketType.REP.type();

	public AbstractZeromqMessageHandler(String url, String clientId) {
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		this.url = url;
		this.clientId = clientId;
	}

	/**
	 * Set the topic to which the message will be published.
	 * @param topic the topic.
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return this.topic;
	}

	/**
	 * Set the message converter to use.
	 * @param converter the converter.
	 */
	public void setConverter(MessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	protected MessageConverter getConverter() {
		return this.converter;
	}

	protected String getUrl() {
		return this.url;
	}

	public String getClientId() {
		return this.clientId;
	}

	public int getClientType() {
		return this.clientType;
	}

	@Override
	public String getComponentType() {
		return "zeromq:outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.topicProcessor instanceof BeanFactoryAware && getBeanFactory() != null) {
			((BeanFactoryAware) this.topicProcessor).setBeanFactory(getBeanFactory());
		}
		if (this.converter == null) {
			org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter defaultConverter = new org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter();
			if (getBeanFactory() != null) {
				defaultConverter.setBeanFactory(getBeanFactory());
			}
			this.converter = defaultConverter;
		}
	}

	@Override
	public final void start() {
		if (!this.running.getAndSet(true)) {
			doStart();
		}
	}

	protected abstract void doStart();

	@Override
	public final void stop() {
		if (this.running.getAndSet(false)) {
			doStop();
		}
	}

	protected abstract void doStop();

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object zmqMessage = this.converter.fromMessage(message, Object.class);
		String currentTopic = this.topicProcessor.processMessage(message);

		this.publish(currentTopic == null ? this.topic : currentTopic, zmqMessage, message);
	}

	protected abstract void publish(String topic, Object zmqMessage, Message<?> message);
}
