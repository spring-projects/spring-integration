/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.webflux.dsl;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.integration.http.dsl.HttpInboundEndpointSupportSpec;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;

/**
 * The {@link HttpInboundEndpointSupportSpec} implementation for the {@link WebFluxInboundEndpoint}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class WebFluxInboundEndpointSpec
		extends HttpInboundEndpointSupportSpec<WebFluxInboundEndpointSpec, WebFluxInboundEndpoint> {

	protected WebFluxInboundEndpointSpec(WebFluxInboundEndpoint gateway, String... path) {
		super(gateway, path);
	}

	public WebFluxInboundEndpointSpec codecConfigurer(ServerCodecConfigurer codecConfigurer) {
		this.target.setCodecConfigurer(codecConfigurer);
		return this;
	}

	public WebFluxInboundEndpointSpec requestedContentTypeResolver(
			RequestedContentTypeResolver requestedContentTypeResolver) {

		this.target.setRequestedContentTypeResolver(requestedContentTypeResolver);
		return this;
	}

	public WebFluxInboundEndpointSpec reactiveAdapterRegistry(ReactiveAdapterRegistry adapterRegistry) {
		this.target.setReactiveAdapterRegistry(adapterRegistry);
		return this;
	}

}
