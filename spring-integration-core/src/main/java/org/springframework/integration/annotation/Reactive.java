/*
 * Copyright 2017 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.integration.reactive.BackpressureType;

import reactor.core.publisher.BufferOverflowStrategy;

/**
 * Provides backpressure options for the Messaging annotations for
 * reactive endpoints.
 * <p>
 * It is an analogue of the XML {@code <reactive/>} element.
 * <p>
 * Non-reference attributes support Property Placeholder resolutions.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see reactor.core.publisher.Flux#onBackpressureBuffer
 * @see reactor.core.publisher.Flux#onBackpressureDrop
 * @see reactor.core.publisher.Flux#onBackpressureError
 * @see reactor.core.publisher.Flux#onBackpressureLatest
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Reactive {

	/**
	 * @return The {@link BackpressureType} to use.
	 */
	@AliasFor("backpressure")
	BackpressureType value() default BackpressureType.NONE;

	/**
	 * @return The {@link BackpressureType} to use.
	 */
	@AliasFor("value")
	BackpressureType backpressure() default BackpressureType.NONE;

	/**
	 * @return The {@link java.util.function.Consumer} bean name, called as a callback on backpressure.
	 */
	String consumer() default "";

	/**
	 * @return The {@link BufferOverflowStrategy} which is used
	 * in case of {@link BackpressureType#BUFFER} for the {@link #backpressure()}.
	 */
	BufferOverflowStrategy bufferOverflowStrategy() default BufferOverflowStrategy.ERROR;

	/**
	 * @return the maximum buffer backlog size before immediate error
	 * in case of {@link BackpressureType#BUFFER} for the {@link #backpressure()}.
	 * Defaults to {@link Integer#MIN_VALUE} meaning {@code unbounded}.
	 */
	String bufferMaxSize() default "-2147483648";

}
