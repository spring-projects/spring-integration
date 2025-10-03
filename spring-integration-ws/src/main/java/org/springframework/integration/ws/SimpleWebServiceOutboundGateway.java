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

import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.support.destination.DestinationProvider;

/**
 * An outbound Messaging Gateway for invoking a Web Service.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Jooyoung Pyoung
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.ws.outbound.SimpleWebServiceOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class SimpleWebServiceOutboundGateway
		extends org.springframework.integration.ws.outbound.SimpleWebServiceOutboundGateway {

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider) {
		this(destinationProvider, null, null);
	}

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider,
			SourceExtractor<?> sourceExtractor) {

		this(destinationProvider, sourceExtractor, null);
	}

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider,
			@Nullable SourceExtractor<?> sourceExtractor,
			@Nullable WebServiceMessageFactory messageFactory) {

		super(destinationProvider, sourceExtractor, messageFactory);
	}

	public SimpleWebServiceOutboundGateway(String uri) {
		this(uri, null, null);
	}

	public SimpleWebServiceOutboundGateway(String uri, SourceExtractor<?> sourceExtractor) {
		this(uri, sourceExtractor, null);
	}

	public SimpleWebServiceOutboundGateway(@Nullable String uri, @Nullable SourceExtractor<?> sourceExtractor,
			@Nullable WebServiceMessageFactory messageFactory) {

		super(uri, sourceExtractor, messageFactory);
	}

}
