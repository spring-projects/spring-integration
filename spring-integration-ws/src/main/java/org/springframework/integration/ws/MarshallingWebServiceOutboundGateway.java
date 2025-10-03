/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ws;

import org.jspecify.annotations.Nullable;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;

/**
 * An outbound Messaging Gateway for invoking Web Services that also supports
 * marshalling and unmarshalling of the request and response messages.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @see Marshaller
 * @see Unmarshaller
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.ws.outbound.MarshallingWebServiceOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class MarshallingWebServiceOutboundGateway
		extends org.springframework.integration.ws.outbound.MarshallingWebServiceOutboundGateway {

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller,
			@Nullable Unmarshaller unmarshaller, @Nullable WebServiceMessageFactory messageFactory) {

		super(destinationProvider, marshaller, unmarshaller, messageFactory);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller,
			Unmarshaller unmarshaller) {

		this(destinationProvider, marshaller, unmarshaller, null);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller,
			@Nullable WebServiceMessageFactory messageFactory) {

		this(destinationProvider, marshaller, null, messageFactory);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller) {
		this(destinationProvider, marshaller, (WebServiceMessageFactory) null);
	}

	public MarshallingWebServiceOutboundGateway(@Nullable String uri, Marshaller marshaller,
			@Nullable Unmarshaller unmarshaller, @Nullable WebServiceMessageFactory messageFactory) {

		super(uri, marshaller, unmarshaller, messageFactory);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller, Unmarshaller unmarshaller) {
		this(uri, marshaller, unmarshaller, null);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller,
			@Nullable WebServiceMessageFactory messageFactory) {
		this(uri, marshaller, null, messageFactory);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller) {
		this(uri, marshaller, (WebServiceMessageFactory) null);
	}

	/**
	 * Construct an instance based on the provided Web Service URI and {@code WebServiceTemplate}.
	 * @param uri the Web Service URI to use
	 * @param webServiceTemplate the WebServiceTemplate
	 * @since 5.0
	 */
	public MarshallingWebServiceOutboundGateway(@Nullable String uri, WebServiceTemplate webServiceTemplate) {
		super(uri, webServiceTemplate);
	}

	/**
	 * Construct an instance based on the provided {@code DestinationProvider} and {@code WebServiceTemplate}.
	 * @param destinationProvider the {@link DestinationProvider} to resolve Web Service URI at runtime
	 * @param webServiceTemplate the WebServiceTemplate
	 * @since 5.0
	 */
	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider,
			WebServiceTemplate webServiceTemplate) {

		super(destinationProvider, webServiceTemplate);
	}

}
