/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.integration.rsocket;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * A base connector container for common RSocket client and server functionality.
 * <p>
 * It accepts {@link IntegrationRSocketEndpoint} instances for mapping registration via an internal
 * {@link IntegrationRSocketMessageHandler} or performs an auto-detection otherwise, when all beans are ready
 * in the application context.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see IntegrationRSocketMessageHandler
 */
public abstract class AbstractRSocketConnector
		implements ApplicationContextAware, InitializingBean, DisposableBean, SmartInitializingSingleton,
		SmartLifecycle {

	protected final IntegrationRSocketMessageHandler rSocketMessageHandler; // NOSONAR - final

	private boolean autoStartup = true;

	private volatile boolean running;

	protected AbstractRSocketConnector(IntegrationRSocketMessageHandler rSocketMessageHandler) {
		Assert.notNull(rSocketMessageHandler, "'rSocketMessageHandler' must not be null");
		this.rSocketMessageHandler = rSocketMessageHandler;
	}

	/**
	 * Configure a {@link MimeType} for data exchanging.
	 * @param dataMimeType the {@link MimeType} to use.
	 */
	public void setDataMimeType(@Nullable MimeType dataMimeType) {
		this.rSocketMessageHandler.setDefaultDataMimeType(dataMimeType);
	}

	@Nullable
	protected MimeType getDataMimeType() {
		return this.rSocketMessageHandler.getDefaultDataMimeType();
	}

	/**
	 * Configure a {@link MimeType} for metadata exchanging.
	 * Default to {@code "message/x.rsocket.composite-metadata.v0"}.
	 * @param metadataMimeType the {@link MimeType} to use.
	 */
	public void setMetadataMimeType(MimeType metadataMimeType) {
		this.rSocketMessageHandler.setDefaultMetadataMimeType(metadataMimeType);
	}

	protected MimeType getMetadataMimeType() {
		return this.rSocketMessageHandler.getDefaultMetadataMimeType();
	}

	/**
	 * Configure a {@link RSocketStrategies} for data encoding/decoding.
	 * @param rsocketStrategies the {@link RSocketStrategies} to use.
	 */
	public void setRSocketStrategies(RSocketStrategies rsocketStrategies) {
		this.rSocketMessageHandler.setRSocketStrategies(rsocketStrategies);
	}

	public RSocketStrategies getRSocketStrategies() {
		return this.rSocketMessageHandler.getRSocketStrategies();
	}

	/**
	 * Configure {@link IntegrationRSocketEndpoint} instances for mapping and handling requests.
	 * @param endpoints the {@link IntegrationRSocketEndpoint} instances for handling inbound requests.
	 * @see #addEndpoint(IntegrationRSocketEndpoint)
	 */
	public void setEndpoints(IntegrationRSocketEndpoint... endpoints) {
		Assert.notNull(endpoints, "'endpoints' must not be null");
		for (IntegrationRSocketEndpoint endpoint : endpoints) {
			addEndpoint(endpoint);
		}
	}

	/**
	 * Add an {@link IntegrationRSocketEndpoint} for mapping and handling RSocket requests.
	 * @param endpoint the {@link IntegrationRSocketEndpoint} to map.
	 */
	public void addEndpoint(IntegrationRSocketEndpoint endpoint) {
		this.rSocketMessageHandler.addEndpoint(endpoint);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.rSocketMessageHandler.setApplicationContext(applicationContext);
	}

	@Override
	public void afterPropertiesSet() {
		this.rSocketMessageHandler.afterPropertiesSet();
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.rSocketMessageHandler.detectEndpoints();
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void start() {
		if (!this.running) {
			this.running = true;
			doStart();
		}
	}

	protected abstract void doStart();

	@Override
	public void stop() {
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
