/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.integration.router.AggregatingMessageHandler;

/**
 * Indicates that a method is capable of aggregating messages. 
 * 
 * @author Marius Bogoevici
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Handler
public @interface Aggregator {

	String defaultReplyChannel() default "";

	String discardChannel() default "";

	long sendTimeout() default AggregatingMessageHandler.DEFAULT_SEND_TIMEOUT;

	long timeout() default AggregatingMessageHandler.DEFAULT_TIMEOUT;

	boolean sendPartialResultsOnTimeout() default false;

	long reaperInterval() default AggregatingMessageHandler.DEFAULT_REAPER_INTERVAL;

	int trackedCorrelationIdCapacity() default AggregatingMessageHandler.DEFAULT_TRACKED_CORRRELATION_ID_CAPACITY; 

}
