/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.http.support;

import org.springframework.util.ClassUtils;

/**
 * Utility class for accessing HTTP integration components
 * from the {@link org.springframework.beans.factory.BeanFactory}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public final class HttpContextUtils {

	private HttpContextUtils() {
		super();
	}

	/**
	 * The {@code boolean} flag to indicate if the {@code org.springframework.web.servlet.DispatcherServlet}
	 * is present in the CLASSPATH to allow to register the Integration server components,
	 * e.g. {@code IntegrationGraphController}.
	 */
	public static final boolean SERVLET_PRESENT =
			ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet",
			HttpContextUtils.class.getClassLoader());

	/**
	 * @see org.springframework.integration.http.config.HttpInboundEndpointParser
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "integrationRequestMappingHandlerMapping";

	/**
	 * Represents the environment property for the {@code IntegrationGraphController} request mapping path.
	 */
	public static final String GRAPH_CONTROLLER_PATH_PROPERTY =
			"spring.integration.graph.controller.request.mapping.path";

	/**
	 * Represents the default request mapping path for the {@code IntegrationGraphController}.
	 */
	public static final String GRAPH_CONTROLLER_DEFAULT_PATH = "/integration";

	/**
	 * Represents the bean name for the default {@code IntegrationGraphController}.
	 */
	public static final String GRAPH_CONTROLLER_BEAN_NAME = "integrationGraphController";

}
