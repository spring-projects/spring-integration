/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.webflux.support;

import org.springframework.integration.http.support.HttpContextUtils;

/**
 * Utility class for accessing WebFlux integration components
 * from the {@link org.springframework.beans.factory.BeanFactory}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class WebFluxContextUtils {

	private WebFluxContextUtils() {
		super();
	}

	/**
	 * The {@code boolean} flag to indicate if the
	 * {@code org.springframework.web.reactive.result.method.RequestMappingInfo}
	 * is present in the CLASSPATH to allow to register the Integration server reactive components.
	 * @deprecated since 5.0.2 in favor of {@link HttpContextUtils#WEB_FLUX_PRESENT}.
	 * Will be removed in the 5.1.
	 */
	@Deprecated
	public static final boolean WEB_FLUX_PRESENT = HttpContextUtils.WEB_FLUX_PRESENT;

	/**
	 * The name for the infrastructure
	 * {@link org.springframework.integration.webflux.inbound.WebFluxIntegrationRequestMappingHandlerMapping} bean.
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "webFluxIntegrationRequestMappingHandlerMapping";

}
