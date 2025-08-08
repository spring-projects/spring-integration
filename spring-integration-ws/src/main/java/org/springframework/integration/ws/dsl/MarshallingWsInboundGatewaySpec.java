/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.ws.dsl;

import org.springframework.integration.ws.MarshallingWebServiceInboundGateway;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;

/**
 * The spec for a {@link MarshallingWebServiceInboundGateway}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.3
 *
 */
public class MarshallingWsInboundGatewaySpec
		extends BaseWsInboundGatewaySpec<MarshallingWsInboundGatewaySpec, MarshallingWebServiceInboundGateway> {

	protected MarshallingWsInboundGatewaySpec() {
		super(new MarshallingWebServiceInboundGateway());
	}

	/**
	 * Specify a marshaller to use.
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public MarshallingWsInboundGatewaySpec marshaller(Marshaller marshaller) {
		this.target.setMarshaller(marshaller);
		if (marshaller instanceof Unmarshaller unmarshaller) {
			return unmarshaller(unmarshaller);
		}
		return this;
	}

	/**
	 * Specify an unmarshaller to use. Required if the {@link #marshaller} is not also
	 * an {@link Unmarshaller}.
	 * @param unmarshaller the unmarshaller.
	 * @return the spec.
	 */
	public MarshallingWsInboundGatewaySpec unmarshaller(Unmarshaller unmarshaller) {
		this.target.setUnmarshaller(unmarshaller);
		return this;
	}

}
