/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.webflux.support;

/**
 * Utility class for accessing WebFlux integration components
 * from the {@link org.springframework.beans.factory.BeanFactory}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public final class WebFluxContextUtils {

	private WebFluxContextUtils() {
	}

	/**
	 * The name for the infrastructure
	 * {@link org.springframework.integration.webflux.inbound.WebFluxIntegrationRequestMappingHandlerMapping} bean.
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "webFluxIntegrationRequestMappingHandlerMapping";

}
