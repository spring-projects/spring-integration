/*
 * Copyright 2016-2021 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Enables the
 * {@link org.springframework.integration.http.management.IntegrationGraphController} if
 * {@code org.springframework.web.servlet.DispatcherServlet} or
 * {@code org.springframework.web.reactive.DispatcherHandler} is present in the classpath.
 *
 * @author Artem Bilan
 *
 * @since 4.3
 *
 * @see org.springframework.integration.http.management.IntegrationGraphController
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(IntegrationGraphControllerRegistrar.class)
public @interface EnableIntegrationGraphController {

	/**
	 * Specify the Request Mapping path for the
	 * {@link org.springframework.integration.http.management.IntegrationGraphController}.
	 * Defaults to {@value HttpContextUtils#GRAPH_CONTROLLER_DEFAULT_PATH}.
	 * @return The Request Mapping path for the
	 * {@link org.springframework.integration.http.management.IntegrationGraphController}
	 */
	@AliasFor("path")
	String value() default HttpContextUtils.GRAPH_CONTROLLER_DEFAULT_PATH;

	/**
	 * Specify the Request Mapping path for the
	 * {@link org.springframework.integration.http.management.IntegrationGraphController}.
	 * Defaults to {@value HttpContextUtils#GRAPH_CONTROLLER_DEFAULT_PATH}.
	 * @return The Request Mapping path for the
	 * {@link org.springframework.integration.http.management.IntegrationGraphController}
	 */
	@AliasFor("value")
	String path() default HttpContextUtils.GRAPH_CONTROLLER_DEFAULT_PATH;

	/**
	 * Specify allowed origin URLs for cross-origin request handling.
	 * Only allows GET operations.
	 * @return the URLs.
	 * @since 4.3.5
	 */
	String[] allowedOrigins() default { };

}
