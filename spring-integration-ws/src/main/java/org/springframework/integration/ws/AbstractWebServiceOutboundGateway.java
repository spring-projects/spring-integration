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
import org.springframework.ws.client.support.destination.DestinationProvider;

/**
 * Base class for outbound Web Service-invoking Messaging Gateways.
 *
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Ngoc Nhan
 * @author Jooyoung Pyoung
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.ws.outbound.AbstractWebServiceOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public abstract class AbstractWebServiceOutboundGateway
		extends org.springframework.integration.ws.outbound.AbstractWebServiceOutboundGateway {

	public AbstractWebServiceOutboundGateway(@Nullable final String uri,
			@Nullable WebServiceMessageFactory messageFactory) {

		super(uri, messageFactory);
	}

	public AbstractWebServiceOutboundGateway(DestinationProvider destinationProvider,
			@Nullable WebServiceMessageFactory messageFactory) {

		super(destinationProvider, messageFactory);
	}

}
