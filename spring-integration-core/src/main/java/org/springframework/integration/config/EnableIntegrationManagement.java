/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.integration.support.management.micrometer.MicrometerMetricsCaptorRegistrar;

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
@Import({ MicrometerMetricsCaptorRegistrar.class, IntegrationManagementConfiguration.class })
public @interface EnableIntegrationManagement {

	/**
	 * @deprecated this property is no longer used.
	 * @return the patterns.
	 */
	@Deprecated
	String[] metersEnabled() default "*";

	/**
	 * @deprecated this property is no longer used.
	 * @return the value; false by default, or true when JMX is enabled.
	 */
	@Deprecated
	String defaultCountsEnabled() default "false";

	/**
	 * Use to disable all logging in the main message flow in framework components. When 'false', such logging will be
	 * skipped, regardless of logging level. When 'true', the logging is controlled as normal by the logging
	 * subsystem log level configuration.
	 * <p>
	 * It has been found that in high-volume messaging environments, calls to methods such as
	 * {@code logger.isDebuggingEnabled()} can be quite expensive and account for an inordinate amount of CPU
	 * time.
	 * <p>
	 * Set this to false to disable logging by default in all framework components that implement
	 * {@link org.springframework.integration.support.management.IntegrationManagement}
	 * (channels, message handlers etc). This turns off logging such as
	 * "PreSend on channel", "Received message" etc.
	 * <p>
	 * After the context is initialized, individual components can have their setting changed by invoking
	 * {@link org.springframework.integration.support.management.IntegrationManagement#setLoggingEnabled(boolean)}.
	 * @return the value; true by default.
	 */
	String defaultLoggingEnabled() default "true";

}
