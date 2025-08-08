/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
	String[] allowedOrigins() default {};

}
