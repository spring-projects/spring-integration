/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.ws.dsl;

import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.ws.AbstractWebServiceInboundGateway;
import org.springframework.integration.ws.SoapHeaderMapper;

/**
 * Base {@link MessagingGatewaySpec} for web services.
 *
 * @param <S> the target {@link BaseWsInboundGatewaySpec} implementation type.
 * @param <E> the target {@link AbstractWebServiceInboundGateway} implementation type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.3
 *
 */
public abstract class BaseWsInboundGatewaySpec<
		S extends BaseWsInboundGatewaySpec<S, E>, E extends AbstractWebServiceInboundGateway>
		extends MessagingGatewaySpec<S, E> {

	/**
	 * Construct an instance based on the provided {@link AbstractWebServiceInboundGateway}.
	 */
	protected BaseWsInboundGatewaySpec(E gateway) {
		super(gateway);
	}

	/**
	 * Configure the header mapper.
	 * @param headerMapper the mapper.
	 * @return the spec.
	 */
	public S headerMapper(SoapHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

}
