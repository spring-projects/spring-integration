/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Utility class for accessing HTTP integration components
 * from the {@link org.springframework.beans.factory.BeanFactory}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public final class HttpContextUtils {

	private HttpContextUtils() {
	}

	/**
	 * A {@code boolean} flag to indicate if the
	 * {@code org.springframework.web.servlet.DispatcherServlet} is present in the
	 * CLASSPATH to allow the registration of Integration server components,
	 * e.g. {@code IntegrationGraphController}.
	 */
	public static final boolean WEB_MVC_PRESENT =
			ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet", null);

	/**
	 * A {@code boolean} flag to indicate if the
	 * {@code org.springframework.web.reactive.DispatcherHandler} is present in the
	 * CLASSPATH to allow the registration of Integration server reactive components,
	 * e.g. {@code IntegrationGraphController}.
	 */
	public static final boolean WEB_FLUX_PRESENT =
			ClassUtils.isPresent("org.springframework.web.reactive.DispatcherHandler", null);

	/**
	 * The name for the infrastructure
	 * {@link org.springframework.integration.http.inbound.IntegrationRequestMappingHandlerMapping} bean.
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

	/**
	 * Converts a provided {@link org.springframework.integration.http.inbound.RequestMapping}
	 * to the Spring Web {@link RequestMapping} annotation.
	 * @param requestMapping the {@link org.springframework.integration.http.inbound.RequestMapping} to convert.
	 * @return the {@link RequestMapping} annotation.
	 * @since 5.0
	 */
	public static RequestMapping convertRequestMappingToAnnotation(
			org.springframework.integration.http.inbound.RequestMapping requestMapping) {

		if (ObjectUtils.isEmpty(requestMapping.getPathPatterns())) {
			return null;
		}

		Map<String, Object> requestMappingAttributes = new HashMap<>();
		requestMappingAttributes.put("name", requestMapping.getName());
		requestMappingAttributes.put("value", requestMapping.getPathPatterns());
		requestMappingAttributes.put("path", requestMapping.getPathPatterns());
		requestMappingAttributes.put("method", requestMapping.getRequestMethods());
		requestMappingAttributes.put("params", requestMapping.getParams());
		requestMappingAttributes.put("headers", requestMapping.getHeaders());
		requestMappingAttributes.put("consumes", requestMapping.getConsumes());
		requestMappingAttributes.put("produces", requestMapping.getProduces());

		return AnnotationUtils.synthesizeAnnotation(requestMappingAttributes, RequestMapping.class, null);
	}

}
