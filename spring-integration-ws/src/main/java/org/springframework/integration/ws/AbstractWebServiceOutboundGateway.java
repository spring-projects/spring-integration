/*
 * Copyright 2002-present the original author or authors.
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
