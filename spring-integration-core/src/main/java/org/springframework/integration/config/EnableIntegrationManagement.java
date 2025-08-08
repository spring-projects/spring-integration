/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.integration.support.management.micrometer.MicrometerMetricsCaptorImportSelector;

/**
 * Enables default configuring of management in Spring Integration components in an existing application.
 *
 * <p>The resulting {@link IntegrationManagementConfigurer}
 * bean is defined under the name {@code integrationManagementConfigurer}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({MicrometerMetricsCaptorImportSelector.class, IntegrationManagementConfiguration.class})
public @interface EnableIntegrationManagement {

	/**
	 * Use for disabling all logging in the main message flow in framework components. When 'false',
	 * such logging will be skipped, regardless of logging level. When 'true',
	 * the logging is controlled as normal by the logging subsystem log level configuration.
	 * <p>
	 * It has been found that in high-volume messaging environments, calls to methods such as
	 * {@code logger.isDebuggingEnabled()} can be quite expensive and account for an inordinate amount of CPU
	 * time.
	 * <p>
	 * Set this to false for disabling logging by default in all framework components that implement
	 * {@link org.springframework.integration.support.management.IntegrationManagement}
	 * (channels, message handlers etc.). It turns off logging such as "PreSend on channel", "Received message" etc.
	 * <p>
	 * After the context is initialized, individual components can have their setting changed by invoking
	 * {@link org.springframework.integration.support.management.IntegrationManagement#setLoggingEnabled(boolean)}.
	 * @return the value; true by default.
	 */
	String defaultLoggingEnabled() default "true";

	/**
	 * Set simple pattern component names matching for observation registry injection.
	 * @return simple pattern component names matching for observation registry injection.
	 * None by default - no unconditional observation instrumentation.
	 * Can be set to {@code *} to instrumentation all the integration components.
	 * The pattern can start with {@code !} to negate the matching.
	 * The value can be a property placeholder and/or comma-separated.
	 * @since 6.0
	 * @see org.springframework.integration.support.utils.PatternMatchUtils#smartMatch(String, String...)
	 */
	String[] observationPatterns() default {};

}
