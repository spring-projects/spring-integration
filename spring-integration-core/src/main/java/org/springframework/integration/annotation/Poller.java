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
 * Provides the {@link PollerMetadata} options for the Messaging annotations.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target({ })
@Retention(RUNTIME)
public @interface Poller {

	/**
	 * @return The {@link PollerMetadata} bean name.
	 */
	String ref() default "";

	/**
	 * @return The maximum number of messages to receive for each poll.
	 */
	long maxMessagesPerPoll() default PollerMetadata.MAX_MESSAGES_UNBOUNDED;

	/**
	 * @return The fixed rate in milliseconds to create the {@link PeriodicTrigger}.
	 */
	long fixedDelay() default -1;

	/**
	 * @return The fixed rate in milliseconds to create the {@link PeriodicTrigger} with {@code fixedRate}.
	 */
	long fixedRate() default -1;

	/**
	 * @return The cron expression to create the {@link CronTrigger}.
	 */
	String cron() default "";

	/**
	 * @return The {@link Trigger} bean name.
	 */
	String trigger() default "";
}
