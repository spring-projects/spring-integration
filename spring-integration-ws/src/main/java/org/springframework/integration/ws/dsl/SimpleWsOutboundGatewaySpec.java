/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.ws.dsl;

import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;
import org.springframework.lang.Nullable;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.support.destination.DestinationProvider;

/**
 * The spec for a {@link SimpleWebServiceOutboundGateway}.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
public class SimpleWsOutboundGatewaySpec extends BaseWsOutboundGatewaySpec<
	SimpleWsOutboundGatewaySpec, SimpleWebServiceOutboundGateway> {

	/**
	 * Construct an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 */
	protected SimpleWsOutboundGatewaySpec(DestinationProvider destinationProvider) {
		this(destinationProvider, null, null);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param sourceExtractor the source extractor.
	 */
	protected SimpleWsOutboundGatewaySpec(DestinationProvider destinationProvider, SourceExtractor<?> sourceExtractor) {
		this(destinationProvider, sourceExtractor, null);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param sourceExtractor the source extractor.
	 * @param messageFactory the message factory.
	 */
	protected SimpleWsOutboundGatewaySpec(DestinationProvider destinationProvider,
			@Nullable SourceExtractor<?> sourceExtractor,
			@Nullable WebServiceMessageFactory messageFactory) {

		this.target = new SimpleWebServiceOutboundGateway(destinationProvider, sourceExtractor, messageFactory);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param uri the URI.
	 */
	protected SimpleWsOutboundGatewaySpec(String uri) {
		this(uri, null, null);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param uri the URI.
	 * @param sourceExtractor the source extractor.
	 */
	protected SimpleWsOutboundGatewaySpec(String uri, SourceExtractor<?> sourceExtractor) {
		this(uri, sourceExtractor, null);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param uri the URI.
	 * @param sourceExtractor the source extractor.
	 * @param messageFactory the message factory.
	 */
	protected SimpleWsOutboundGatewaySpec(String uri,
			@Nullable SourceExtractor<?> sourceExtractor,
			@Nullable WebServiceMessageFactory messageFactory) {

		this.target = new SimpleWebServiceOutboundGateway(uri, sourceExtractor, messageFactory);
	}

	/**
	 * Specify a flag to return the whole {@link WebServiceMessage} or build the
	 * {@code payload} based on {@link WebServiceMessage}
	 * and populated headers according {@code headerMapper} configuration.
	 * Defaults to extract payload.
	 * @param extractPayload build payload or return a whole {@link WebServiceMessage}
	 * @return the spec.
	 */
	public SimpleWsOutboundGatewaySpec extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return this;
	}

}
