/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.ws.dsl;

import org.springframework.integration.ws.SimpleWebServiceInboundGateway;

/**
 * The spec for a {@link SimpleWebServiceInboundGateway}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class SimpleWsInboundGatewaySpec
		extends BaseWsInboundGatewaySpec<SimpleWsInboundGatewaySpec, SimpleWebServiceInboundGateway> {

	protected SimpleWsInboundGatewaySpec() {
		super(new SimpleWebServiceInboundGateway());
	}

	/**
	 * Specify true to extract the payloadSource from the request or use
	 * the entire request as the payload; default true.
	 *
	 * @param extract true to extract.
	 * @return the spec.
	 */
	public SimpleWsInboundGatewaySpec extractPayload(boolean extract) {
		this.target.setExtractPayload(extract);
		return this;
	}

}
