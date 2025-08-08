/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.webflux.support;

import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.web.reactive.function.BodyExtractor;

/**
 * The {@link BodyExtractor} identity function implementation
 * which just returns the provided {@link ClientHttpResponse}.
 *
 * @author Artem Bilan
 *
 * @since 5.0.1
 */
public class ClientHttpResponseBodyExtractor implements BodyExtractor<ClientHttpResponse, ClientHttpResponse> {

	@Override
	public ClientHttpResponse extract(ClientHttpResponse response, Context context) {
		return response;
	}

}
