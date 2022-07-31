/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.integration.mqtt.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

/**
 * @param <T> MQTT client type
 *
 * @author Artem Vozhdayenko
 *
 * @since 6.0
 */
public abstract class AbstractMqttClientManager<T> implements ClientManager<T>, ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(this.getClass()); // NOSONAR

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean manualAcks;

	private String url;

	private final String clientId;

	volatile T client;

	AbstractMqttClientManager(String clientId) {
		Assert.notNull(clientId, "'clientId' is required");
		this.clientId = clientId;
	}

	protected void setManualAcks(boolean manualAcks) {
		this.manualAcks = manualAcks;
	}

	protected String getUrl() {
		return this.url;
	}

	protected void setUrl(String url) {
		this.url = url;
	}

	protected String getClientId() {
		return this.clientId;
	}

	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	@Override
	public boolean isManualAcks() {
		return this.manualAcks;
	}

	@Override
	public T getClient() {
		return this.client;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		Assert.notNull(applicationEventPublisher, "'applicationEventPublisher' cannot be null");
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public synchronized boolean isRunning() {
		return this.client != null;
	}
}
