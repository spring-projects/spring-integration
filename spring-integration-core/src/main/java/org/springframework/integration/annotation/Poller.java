/*
 * Copyright 2014-2023 the original author or authors.
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

package org.springframework.integration.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides the {@link org.springframework.integration.scheduling.PollerMetadata} options
 * for the Messaging annotations for polled endpoints. It is an analogue of the XML
 * {@code <poller/>} element, but provides only simple attributes. If the
 * {@link org.springframework.integration.scheduling.PollerMetadata} requires more options
 * (e.g. Transactional and other Advices) or {@code initialDelay} etc., the
 * {@link org.springframework.integration.scheduling.PollerMetadata} should be configured
 * as a generic bean and its bean name can be specified as the {@code value} attribute of
 * this annotation. In that case, the other attributes are not allowed.
 * <p>
 * Non-reference attributes support Property Placeholder resolutions.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Poller {

	/**
	 * @return The {@link org.springframework.integration.scheduling.PollerMetadata} bean
	 * name.
	 */
	String value() default "";

	/**
	 * @return The {@link org.springframework.scheduling.Trigger} bean name.
	 */
	String trigger() default "";

	/**
	 * @return The {@link org.springframework.core.task.TaskExecutor} bean name.
	 */
	String taskExecutor() default "";

	/**
	 * @return The maximum number of messages to receive for each poll.
	 * Can be specified as 'property placeholder', e.g. {@code ${poller.maxMessagesPerPoll}}.
	 * Defaults to -1 (infinity) for polling consumers and 1 for polling inbound channel adapters.
	 */
	String maxMessagesPerPoll() default "";

	/**
	 * @return The fixed delay in milliseconds or a {@link java.time.Duration} compliant string
	 * to create the {@link org.springframework.scheduling.support.PeriodicTrigger}.
	 * Can be specified as 'property placeholder', e.g. {@code ${poller.fixedDelay}}.
	 */
	String fixedDelay() default "";

	/**
	 * @return The fixed rate in milliseconds or a {@link java.time.Duration} compliant string
	 * to create the {@link org.springframework.scheduling.support.PeriodicTrigger} with
	 * the {@code fixedRate} option. Can be specified as 'property placeholder', e.g.
	 * {@code ${poller.fixedRate}}.
	 */
	String fixedRate() default "";

	/**
	 * @return The cron expression to create the
	 * {@link org.springframework.scheduling.support.CronTrigger}. Can be specified as
	 * 'property placeholder', e.g. {@code ${poller.cron}}.
	 */
	String cron() default "";

	/**
	 * @return The bean name of default error channel
	 * for the underlying {@code MessagePublishingErrorHandler}.
	 * @since 4.3.3
	 */
	String errorChannel() default "";

	/**
	 * Only applies to polling consumers.
	 * @return the time the poll thread will wait after the trigger for a new message to arrive.
	 * Defaults to 1000 (1 second).
	 * For polled inbound channel adapters, whether the polling thread blocks
	 * is dependent on the message source implementation.
	 * Can be specified as 'property placeholder', e.g. {@code ${my.poller.receiveTimeout}}.
	 * @since 5.1
	 */
	String receiveTimeout() default "";

}
