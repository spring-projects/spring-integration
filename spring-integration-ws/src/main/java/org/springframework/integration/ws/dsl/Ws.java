/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.ws.dsl;

import org.springframework.oxm.Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Factory class for web service components.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
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
	 * {@link org.springframework.oxm.Unmarshaller}).
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public static MarshallingWsInboundGatewaySpec marshallingInboundGateway(Marshaller marshaller) {
		MarshallingWsInboundGatewaySpec spec = new MarshallingWsInboundGatewaySpec();
		spec.marshaller(marshaller);
		return spec;
	}

	/**
	 * Create an instance.
	 * @return the spec.
	 */
	public static SimpleWsInboundGatewaySpec simpleInboundGateway() {
		return new SimpleWsInboundGatewaySpec();
	}

	/**
	 * Create an instance with a default {@link WebServiceTemplate}.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec.MarshallingWsOutboundGatewayNoTemplateSpec
	marshallingOutboundGateway() {
		return new MarshallingWsOutboundGatewaySpec.MarshallingWsOutboundGatewayNoTemplateSpec();
	}

	/**
	 * Create an instance with the provided {@link WebServiceTemplate}.
	 * @param template the template.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(WebServiceTemplate template) {
		return new MarshallingWsOutboundGatewaySpec(template);
	}

	/**
	 * Create an instance.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec.SimpleWsOutboundGatewayNoTemplateSpec simpleOutboundGateway() {
		return new SimpleWsOutboundGatewaySpec.SimpleWsOutboundGatewayNoTemplateSpec();
	}

	/**
	 * Create an instance with the provided {@link WebServiceTemplate}.
	 * @param template the template.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec simpleOutboundGateway(WebServiceTemplate template) {
		return new SimpleWsOutboundGatewaySpec(template);
	}

	private Ws() {
	}

}
