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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.reactive.SyncHandlerMethodArgumentResolver;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.DefaultMetadataExtractor;
import org.springframework.messaging.rsocket.annotation.support.MetadataExtractor;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;

/**
 * The {@link RSocketMessageHandler} extension for Spring Integration needs.
 * <p>
 * This class adds an {@link IntegrationRSocketEndpoint} beans detection and registration functionality.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see RSocketMessageHandler
 */
class IntegrationRSocketMessageHandler extends RSocketMessageHandler {

	private static final Method HANDLE_MESSAGE_METHOD =
			ReflectionUtils.findMethod(ReactiveMessageHandler.class, "handleMessage", Message.class);

	@Nullable
	private MimeType defaultDataMimeType;

	private MimeType defaultMetadataMimeType = IntegrationRSocket.COMPOSITE_METADATA;

	private MetadataExtractor metadataExtractor;

	IntegrationRSocketMessageHandler() {
		setHandlerPredicate((clazz) -> false);
	}

	/**
	 * Configure the default content type to use for data payloads.
	 * <p>By default this is not set. However a server acceptor will use the
	 * content type from the {@link io.rsocket.ConnectionSetupPayload}, so this is typically
	 * required for clients but can also be used on servers as a fallback.
	 * @param defaultDataMimeType the MimeType to use
	 */
	@Override
	public void setDefaultDataMimeType(@Nullable MimeType defaultDataMimeType) {
		super.setDefaultDataMimeType(defaultDataMimeType);
		this.defaultDataMimeType = defaultDataMimeType;
	}


	/**
	 * Configure the default {@code MimeType} for payload data if the
	 * {@code SETUP} frame did not specify one.
	 * <p>By default this is set to {@code "message/x.rsocket.composite-metadata.v0"}
	 * @param mimeType the MimeType to use
	 */
	@Override
	public void setDefaultMetadataMimeType(MimeType mimeType) {
		super.setDefaultMetadataMimeType(mimeType);
		this.defaultMetadataMimeType = mimeType;
	}

	/**
	 * Configure a {@link MetadataExtractor} to extract the route and possibly
	 * other metadata from the first payload of incoming requests.
	 * <p>By default this is a {@link DefaultMetadataExtractor} with the
	 * configured {@link RSocketStrategies} (and decoders), extracting a route
	 * from {@code "message/x.rsocket.routing.v0"} or {@code "text/plain"}
	 * metadata entries.
	 * @param extractor the extractor to use
	 */
	@Override
	public void setMetadataExtractor(MetadataExtractor extractor) {
		super.setMetadataExtractor(extractor);
		this.metadataExtractor = extractor;
	}

	@Override
	public BiFunction<ConnectionSetupPayload, RSocket, RSocket> clientAcceptor() {
		return this::createRSocket;
	}

	public boolean detectEndpoints() {
		ApplicationContext applicationContext = getApplicationContext();
		if (applicationContext != null && getHandlerMethods().isEmpty()) {
			return applicationContext
					.getBeansOfType(IntegrationRSocketEndpoint.class)
					.values()
					.stream()
					.peek(this::addEndpoint)
					.count() > 0;
		}
		else {
			return false;
		}
	}

	public void addEndpoint(IntegrationRSocketEndpoint endpoint) {
		registerHandlerMethod(endpoint, HANDLE_MESSAGE_METHOD,
				new CompositeMessageCondition(
						new DestinationPatternsMessageCondition(endpoint.getPath(), getRouteMatcher())));
	}

	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		return Collections.singletonList(new MessageHandlerMethodArgumentResolver());
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (this.metadataExtractor == null) {
			DefaultMetadataExtractor extractor = new DefaultMetadataExtractor(getRSocketStrategies()); // NOSONAR
			extractor.metadataToExtract(MimeTypeUtils.TEXT_PLAIN, String.class, MetadataExtractor.ROUTE_KEY);
			this.metadataExtractor = extractor;
		}
	}

	protected IntegrationRSocket createRSocket(ConnectionSetupPayload setupPayload, RSocket rsocket) {
		String mimeType = setupPayload.dataMimeType();
		MimeType dataMimeType =
				StringUtils.hasText(mimeType)
						? MimeTypeUtils.parseMimeType(mimeType)
						: this.defaultDataMimeType;
		Assert.notNull(dataMimeType, "No `dataMimeType` in ConnectionSetupPayload and no default value");
		mimeType = setupPayload.metadataMimeType();
		MimeType metaMimeType =
				StringUtils.hasText(mimeType)
						? MimeTypeUtils.parseMimeType(mimeType)
						: this.defaultMetadataMimeType;
		Assert.notNull(dataMimeType, "No `metadataMimeType` in ConnectionSetupPayload and no default value");
		RSocketStrategies rSocketStrategies = getRSocketStrategies();
		Assert.notNull(rSocketStrategies, "No `rSocketStrategies` provided");
		RSocketRequester requester = RSocketRequester.wrap(rsocket, dataMimeType, metaMimeType, rSocketStrategies);
		return new IntegrationRSocket(this, getRouteMatcher(), requester, dataMimeType, metaMimeType,
				this.metadataExtractor, rSocketStrategies.dataBufferFactory());
	}

	private static final class MessageHandlerMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return true;
		}

		@Override
		public Object resolveArgumentValue(MethodParameter parameter, Message<?> message) {
			return message;
		}

	}

}
