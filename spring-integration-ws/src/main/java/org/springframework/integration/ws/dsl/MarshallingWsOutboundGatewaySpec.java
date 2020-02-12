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

import org.springframework.integration.ws.MarshallingWebServiceOutboundGateway;
import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.support.destination.DestinationProvider;

/**
 * The spec for a {@link MarshallingWebServiceOutboundGateway}.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
public class MarshallingWsOutboundGatewaySpec extends BaseWsOutboundGatewaySpec<
	MarshallingWsOutboundGatewaySpec, MarshallingWebServiceOutboundGateway> {

	/**
	 * Construct an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param marshaller the marshaller.
	 */
	protected MarshallingWsOutboundGatewaySpec(DestinationProvider destinationProvider, Marshaller marshaller) {
		this(destinationProvider, marshaller, null, null);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param marshaller the marshaller.
	 * @param unmarshaller the unmarshaller.
	 */
	protected MarshallingWsOutboundGatewaySpec(DestinationProvider destinationProvider, Marshaller marshaller,
			Unmarshaller unmarshaller) {
		this(destinationProvider, marshaller, unmarshaller, null);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param marshaller the marshaller.
	 * @param messageFactory the message factory.
	 */
	protected MarshallingWsOutboundGatewaySpec(DestinationProvider destinationProvider, Marshaller marshaller,
			WebServiceMessageFactory messageFactory) {

		this(destinationProvider, marshaller, null, messageFactory);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param destinationProvider the destination provider.
	 * @param marshaller the marshaller.
	 * @param unmarshaller the unmarshaller.
	 * @param messageFactory the message factory.
	 */
	protected MarshallingWsOutboundGatewaySpec(DestinationProvider destinationProvider,
			@Nullable Marshaller marshaller, @Nullable Unmarshaller unmarshaller,
			WebServiceMessageFactory messageFactory) {

		this.target = new MarshallingWebServiceOutboundGateway(destinationProvider, marshaller, unmarshaller,
				messageFactory);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param uri the URI.
	 * @param marshaller the marshaller.
	 */
	protected MarshallingWsOutboundGatewaySpec(String uri, Marshaller marshaller) {
		this(uri, marshaller, (WebServiceMessageFactory) null);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param uri the URI.
	 * @param marshaller the marshaller.
	 * @param messageFactory the message factory.
	 */
	protected MarshallingWsOutboundGatewaySpec(String uri, Marshaller marshaller,
			WebServiceMessageFactory messageFactory) {

		this(uri, marshaller, null, messageFactory);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param uri the URI.
	 * @param marshaller the marshaller.
	 * @param unmarshaller the unmarshaller.
	 */
	protected MarshallingWsOutboundGatewaySpec(String uri, Marshaller marshaller, Unmarshaller unmarshaller) {
		this(uri, marshaller, unmarshaller, null);
	}

	/**
	 * Construct an instance with the provided arguments.
	 * @param uri the URI.
	 * @param marshaller the marshaller.
	 * @param unmarshaller the unmarshaller.
	 * @param messageFactory the message factory.
	 */
	protected MarshallingWsOutboundGatewaySpec(String uri,
			@Nullable Marshaller marshaller, @Nullable Unmarshaller unmarshaller,
			WebServiceMessageFactory messageFactory) {

		this.target = new MarshallingWebServiceOutboundGateway(uri, marshaller, unmarshaller, messageFactory);
	}

}
