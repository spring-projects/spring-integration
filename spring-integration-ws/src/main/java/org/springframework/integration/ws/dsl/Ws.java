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

import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.support.destination.DestinationProvider;

/**
 * Factory class for web service components.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
public final class Ws {

	/**
	 * Create an instance.
	 * @return the spec.
	 */
	public static MarshallingWsInboundGatewaySpec marshallingInboundGateway() {
		return new MarshallingWsInboundGatewaySpec();
	}

	/**
	 * Create an instance with the provided {@link Marshaller} (which must also implement
	 * {@link Unmarshaller}).
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public static MarshallingWsInboundGatewaySpec marshallingInboundGateway(Marshaller marshaller) {
		return new MarshallingWsInboundGatewaySpec(marshaller);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param unmarshaller the unmarshaller.
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public static MarshallingWsInboundGatewaySpec marshallingInboundGateway(Marshaller marshaller,
			Unmarshaller unmarshaller) {

		return new MarshallingWsInboundGatewaySpec(marshaller, unmarshaller);
	}

	/**
	 * Create an instance.
	 * @return the spec.
	 */
	public static SimpleWsInboundGatewaySpec simpleInboundGateway() {
		return new SimpleWsInboundGatewaySpec();
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(
			DestinationProvider destinationProvider, Marshaller marshaller) {

		return new MarshallingWsOutboundGatewaySpec(destinationProvider, marshaller);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param marshaller the marshaller.
	 * @param unmarshaller the unmarshaller.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(
			DestinationProvider destinationProvider, Marshaller marshaller,
			Unmarshaller unmarshaller) {

		return new MarshallingWsOutboundGatewaySpec(destinationProvider, marshaller, unmarshaller, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param marshaller the marshaller.
	 * @param messageFactory the message factory.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(
			DestinationProvider destinationProvider, Marshaller marshaller,
			WebServiceMessageFactory messageFactory) {

		return new MarshallingWsOutboundGatewaySpec(destinationProvider, marshaller, null, messageFactory);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param marshaller the marshaller.
	 * @param unmarshaller the unmarshaller.
	 * @param messageFactory the message factory.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(
			DestinationProvider destinationProvider,
			@Nullable Marshaller marshaller, @Nullable Unmarshaller unmarshaller,
			WebServiceMessageFactory messageFactory) {

		return new MarshallingWsOutboundGatewaySpec(destinationProvider, marshaller, unmarshaller,
				messageFactory);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param uri the URI.
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(
			String uri, Marshaller marshaller) {

		return new MarshallingWsOutboundGatewaySpec(uri, marshaller, null, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param uri the URI.
	 * @param marshaller the marshaller.
	 * @param messageFactory the message factory.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(
			String uri, Marshaller marshaller,
			WebServiceMessageFactory messageFactory) {

		return new MarshallingWsOutboundGatewaySpec(uri, marshaller, null, messageFactory);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param uri the URI.
	 * @param marshaller the marshaller.
	 * @param unmarshaller the unmarshaller.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(
			String uri, Marshaller marshaller, Unmarshaller unmarshaller) {

		return new MarshallingWsOutboundGatewaySpec(uri, marshaller, unmarshaller);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param uri the URI.
	 * @param marshaller the marshaller.
	 * @param unmarshaller the unmarshaller.
	 * @param messageFactory the message factory.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(String uri,
			@Nullable Marshaller marshaller, @Nullable Unmarshaller unmarshaller,
			WebServiceMessageFactory messageFactory) {

		return new MarshallingWsOutboundGatewaySpec(uri, marshaller, unmarshaller, messageFactory);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec simpleOutboundGateway(DestinationProvider destinationProvider) {
		return new SimpleWsOutboundGatewaySpec(destinationProvider, null, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param sourceExtractor the source extractor.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec simpleOutboundGateway(DestinationProvider destinationProvider,
			SourceExtractor<?> sourceExtractor) {

		return new SimpleWsOutboundGatewaySpec(destinationProvider, sourceExtractor, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param sourceExtractor the source extractor.
	 * @param messageFactory the message factory.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec simpleOutboundGateway(DestinationProvider destinationProvider,
			@Nullable SourceExtractor<?> sourceExtractor,
			@Nullable WebServiceMessageFactory messageFactory) {

		return new SimpleWsOutboundGatewaySpec(destinationProvider, sourceExtractor, messageFactory);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param uri the URI.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec simpleOutboundGateway(String uri) {
		return new SimpleWsOutboundGatewaySpec(uri, null, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param uri the URI.
	 * @param sourceExtractor the source extractor.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec simpleOutboundGateway(String uri, SourceExtractor<?> sourceExtractor) {
		return new SimpleWsOutboundGatewaySpec(uri, sourceExtractor, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param uri the URI.
	 * @param sourceExtractor the source extractor.
	 * @param messageFactory the message factory.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec simpleOutboundGateway(String uri,
			@Nullable SourceExtractor<?> sourceExtractor,
			@Nullable WebServiceMessageFactory messageFactory) {

		return new SimpleWsOutboundGatewaySpec(uri, sourceExtractor, messageFactory);
	}

	private Ws() {
	}

}
