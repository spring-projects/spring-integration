/*
 * Copyright 2002-2011 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler;

/**
 * Indicates that a method is capable of aggregating messages. 
 * <p>
 * A method annotated with @Aggregator may accept a collection
 * of Messages or Message payloads and should return a single
 * Message or a single Object to be used as a Message payload.
 * 
 * @author Marius Bogoevici
 * @author Oleg Zhurakousky
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Aggregator {

	/**
	 * channel name for receiving messages to be aggregated
	 */
	String inputChannel() default "";

	/**
	 * channel name for sending aggregated result messages
	 */
	String outputChannel() default "";

	/**
	 * channel name for sending discarded messages (due to a timeout)
	 */
	String discardChannel() default "";

	/**
	 * timeout for sending results to the reply target (in milliseconds)
	 */
	long sendTimeout() default AbstractCorrelatingMessageHandler.DEFAULT_SEND_TIMEOUT;

	/**
	 * indicates whether to send an incomplete aggregate on expiry of the message group
	 */
	boolean sendPartialResultsOnExpiry() default false;

}
