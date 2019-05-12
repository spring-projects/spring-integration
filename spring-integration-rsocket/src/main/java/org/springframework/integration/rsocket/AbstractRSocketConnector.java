/*
 * Copyright 2019 the original author or authors.
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
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * A base connector container for common RSocket client and server functionality.
 * <p>
 * It accepts {@link IntegrationRSocketEndpoint} instances for mapping registration via an internal
 * {@link IntegrationRSocketAcceptor} or performs an auto-detection otherwise, when all bean are ready
 * in the application context.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see IntegrationRSocketAcceptor
 */
public abstract class AbstractRSocketConnector
		implements ApplicationContextAware, InitializingBean, DisposableBean, SmartInitializingSingleton,
		SmartLifecycle {

	protected final IntegrationRSocketAcceptor rsocketAcceptor; // NOSONAR - final

	private MimeType dataMimeType = MimeTypeUtils.TEXT_PLAIN;

	private RSocketStrategies rsocketStrategies =
			RSocketStrategies.builder()
					.decoder(StringDecoder.allMimeTypes())
					.encoder(CharSequenceEncoder.allMimeTypes())
					.dataBufferFactory(new DefaultDataBufferFactory())
					.build();

	private volatile boolean running;

	protected AbstractRSocketConnector(IntegrationRSocketAcceptor rsocketAcceptor) {
		this.rsocketAcceptor = rsocketAcceptor;
	}

	/**
	 * Configure a {@link MimeType} for data exchanging.
	 * @param dataMimeType the {@link MimeType} to use.
	 */
	public void setDataMimeType(MimeType dataMimeType) {
		Assert.notNull(dataMimeType, "'dataMimeType' must not be null");
		this.dataMimeType = dataMimeType;
	}

	protected MimeType getDataMimeType() {
		return this.dataMimeType;
	}

	/**
	 * Configure a {@link RSocketStrategies} for data encoding/decoding.
	 * @param rsocketStrategies the {@link RSocketStrategies} to use.
	 */
	public void setRSocketStrategies(RSocketStrategies rsocketStrategies) {
		Assert.notNull(rsocketStrategies, "'rsocketStrategies' must not be null");
		this.rsocketStrategies = rsocketStrategies;
	}

	public RSocketStrategies getRSocketStrategies() {
		return this.rsocketStrategies;
	}

	/**
	 * Configure {@link IntegrationRSocketEndpoint} instances for mapping nad handling requests.
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
		this.rsocketAcceptor.addEndpoint(endpoint);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.rsocketAcceptor.setApplicationContext(applicationContext);
	}

	@Override
	public void afterPropertiesSet() {
		this.rsocketAcceptor.setDefaultDataMimeType(this.dataMimeType);
		this.rsocketAcceptor.setRSocketStrategies(this.rsocketStrategies);
		this.rsocketAcceptor.afterPropertiesSet();
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.rsocketAcceptor.detectEndpoints();
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
