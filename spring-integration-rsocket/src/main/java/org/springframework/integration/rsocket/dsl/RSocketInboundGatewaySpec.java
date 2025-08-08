/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.rsocket.dsl;

import org.springframework.core.ResolvableType;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.rsocket.AbstractRSocketConnector;
import org.springframework.integration.rsocket.RSocketInteractionModel;
import org.springframework.integration.rsocket.inbound.RSocketInboundGateway;
import org.springframework.messaging.rsocket.RSocketStrategies;

/**
 * The {@link MessagingGatewaySpec} implementation for the {@link RSocketInboundGateway}.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketInboundGatewaySpec extends MessagingGatewaySpec<RSocketInboundGatewaySpec, RSocketInboundGateway> {

	protected RSocketInboundGatewaySpec(String... path) {
		super(new RSocketInboundGateway(path));
	}

	/**
	 * Configure a set of {@link RSocketInteractionModel} the endpoint is going to be mapped onto.
	 * @param interactionModels the {@link RSocketInteractionModel}s for mapping.
	 * @return the spec.
	 * @since 5.2.2
	 * @see RSocketInboundGateway#setInteractionModels(RSocketInteractionModel...)
	 */
	public RSocketInboundGatewaySpec interactionModels(RSocketInteractionModel... interactionModels) {
		this.target.setInteractionModels(interactionModels);
		return this;
	}

	/**
	 * Configure an {@link RSocketStrategies} instead of a default one.
	 * @param rsocketStrategies the {@link RSocketStrategies} to use.
	 * @return the spec
	 * @see RSocketInboundGateway#setRSocketStrategies(RSocketStrategies)
	 */
	public RSocketInboundGatewaySpec rsocketStrategies(RSocketStrategies rsocketStrategies) {
		this.target.setRSocketStrategies(rsocketStrategies);
		return this;
	}

	/**
	 * Provide an {@link AbstractRSocketConnector} reference for an explicit endpoint mapping.
	 * @param rsocketConnector the {@link AbstractRSocketConnector} to use.
	 * @return the spec
	 * @see RSocketInboundGateway#setRSocketConnector(AbstractRSocketConnector)
	 */
	public RSocketInboundGatewaySpec rsocketConnector(AbstractRSocketConnector rsocketConnector) {
		this.target.setRSocketConnector(rsocketConnector);
		return this;
	}

	/**
	 * Specify a type of payload to be generated when the inbound RSocket request
	 * content is read by the converters/encoders.
	 * @param requestElementType The payload type.
	 * @return the spec
	 * @see RSocketInboundGateway#setRequestElementType(ResolvableType)
	 */
	public RSocketInboundGatewaySpec requestElementType(ResolvableType requestElementType) {
		this.target.setRequestElementType(requestElementType);
		return this;
	}

	/**
	 * Configure an option to decode an incoming {@link reactor.core.publisher.Flux}
	 * as a single unit or each its event separately.
	 * @param decodeFluxAsUnit decode incoming {@link reactor.core.publisher.Flux}
	 *                         as a single unit or each event separately.
	 * @return the spec
	 * @since 5.3
	 * @see RSocketInboundGateway#setDecodeFluxAsUnit(boolean)
	 */
	public RSocketInboundGatewaySpec decodeFluxAsUnit(boolean decodeFluxAsUnit) {
		this.target.setDecodeFluxAsUnit(decodeFluxAsUnit);
		return this;
	}

}
