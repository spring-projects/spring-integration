/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.http.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Enables the
 * {@link org.springframework.integration.http.management.ControlBusController} if
 * {@code org.springframework.web.servlet.DispatcherServlet} or
 * {@code org.springframework.web.reactive.DispatcherHandler} is present in the classpath.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 *
 * @see org.springframework.integration.http.management.ControlBusController
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(ControlBusControllerConfiguration.class)
public @interface EnableControlBusController {

}
