/*
 * Copyright 2014-2018 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.integration.support.management.IntegrationManagement;

/**
 * Enables default configuring of management in Spring Integration components in an existing application.
 *
 * <p>The resulting {@link IntegrationManagementConfigurer}
 * bean is defined under the name {@code integrationManagementConfigurer}.
 *
 * @author Gary Russell
 *
 * @since 4.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(IntegrationManagementConfiguration.class)
public @interface EnableIntegrationManagement {

	/**
	 * A list of simple patterns for component names for which message counts will be
	 * enabled (defaults to '*'). Enables message
	 * counting (`sendCount`, `errorCount`, `receiveCount`) for those components that
	 * support counters (channels, message handlers, etc). This is the initial setting
	 * only, individual components can have counts enabled/disabled at runtime. May be
	 * overridden by an entry in {@link #statsEnabled() statsEnabled} which is additional
	 * functionality over simple counts. If a pattern starts with `!`, counts are disabled
	 * for matches. For components that match multiple patterns, the first pattern wins.
	 * Disabling counts at runtime also disables stats.
	 * Defaults to no components, unless JMX is enabled in which case, defaults to all
	 * components. Overrides {@link #defaultCountsEnabled()} for matching bean names.
	 * @return the patterns.
	 */
	String[] countsEnabled() default "*";

	/**
	 * A list of simple patterns for component names for which message statistics will be
	 * enabled (response times, rates etc), as well as counts (a positive match here
	 * overrides {@link #countsEnabled() countsEnabled}, you can't have statistics without
	 * counts). (defaults to '*'). Enables
	 * statistics for those components that support statistics (channels - when sending,
	 * message handlers, etc). This is the initial setting only, individual components can
	 * have stats enabled/disabled at runtime. If a pattern starts with `!`, stats (and
	 * counts) are disabled for matches. Note: this means that '!foo' here will disable
	 * stats and counts for 'foo' even if counts are enabled for 'foo' in
	 * {@link #countsEnabled() countsEnabled}. For components
	 * that match multiple patterns, the first pattern wins. Enabling stats at runtime
	 * also enables counts.
	 * Defaults to no components, unless JMX is enabled in which case, defaults to all
	 * components.
	 * @return the patterns.
	 */
	String[] statsEnabled() default "*";

	/**
	 * The default setting for enabling counts when a bean name is not matched by
	 * {@link #countsEnabled() countsEnabled}.
	 * @return the value; false by default, or true when JMX is enabled.
	 */
	String defaultCountsEnabled() default "false";

	/**
	 * The default setting for enabling statistics when a bean name is not matched by
	 * {@link #statsEnabled() statsEnabled}.
	 * @return the value; false by default, or true when JMX is enabled.
	 */
	String defaultStatsEnabled() default "false";

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
	 * {@link IntegrationManagement} (channels, message handlers etc). This turns off logging such as
	 * "PreSend on channel", "Received message" etc.
	 * <p>
	 * After the context is initialized, individual components can have their setting changed by invoking
	 * {@link IntegrationManagement#setLoggingEnabled(boolean)}.
	 * @return the value; true by default.
	 */
	String defaultLoggingEnabled() default "true";

	/**
	 * The bean name of a {@code MetricsFactory}. The {@code DefaultMetricsFactory} is used
	 * if omitted.
	 * @return the bean name.
	 */
	String metricsFactory() default "";

}
