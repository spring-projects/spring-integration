/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.Trigger;

/**
 * Provides the {@link PollerMetadata} options for the Messaging annotations for
 * polled endpoints.
 * It is an analogue of the XML {@code <poller/>} element, but provides only simple attributes.
 * If the {@link org.springframework.integration.scheduling.PollerMetadata} requires more options
 * (e.g. Transactional and other Advices) or {@code initialDelay, receiveTimeout} etc,
 * the {@link org.springframework.integration.scheduling.PollerMetadata} should be configured as
 * a generic bean and its bean name can be specified as the {@code value} attribute of this annotation.
 * In that case, the other attributes are not allowed.
 * <p>
 * Non-reference attributes support Property Placeholder resolutions.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target({})
@Retention(RUNTIME)
public @interface Poller {

	/**
	 * @return The {@link PollerMetadata} bean name.
	 */
	String value() default "";

	/**
	 * @return The {@link Trigger} bean name.
	 */
	String trigger() default "";

	/**
	 * @return The {@link org.springframework.core.task.TaskExecutor} bean name.
	 */
	String taskExecutor() default "";

	/**
	 * @return The maximum number of messages to receive for each poll.
	 * Can be specified as 'property placeholder', e.g. {@code ${poller.maxMessagesPerPoll}}.
	 */
	String maxMessagesPerPoll() default "";

	/**
	 * @return The fixed delay in milliseconds to create the {@link PeriodicTrigger}.
	 * Can be specified as 'property placeholder', e.g. {@code ${poller.fixedDelay}}.
	 */
	String fixedDelay() default "";

	/**
	 * @return The fixed rate in milliseconds to create the {@link PeriodicTrigger} with {@code fixedRate}.
	 * Can be specified as 'property placeholder', e.g. {@code ${poller.fixedRate}}.
	 */
	String fixedRate() default "";

	/**
	 * @return The cron expression to create the {@link CronTrigger}.
	 * Can be specified as 'property placeholder', e.g. {@code ${poller.cron}}.
	 */
	String cron() default "";
}
