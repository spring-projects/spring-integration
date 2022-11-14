/*
 * Copyright 2014-2022 the original author or authors.
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

/**
 * {@link org.springframework.messaging.support.ChannelInterceptor} components with this
 * annotation will be applied as global channel interceptors
 * using the provided {@code patterns} to match channel names.
 * <p>
 * This annotation can be used at the {@code class} level
 * for {@link org.springframework.stereotype.Component} beans
 * and on methods with {@link org.springframework.context.annotation.Bean}.
 * <p>
 * This annotation is an analogue of {@code <int:channel-interceptor/>}.
 *
 * @author Artem Bilan
 * @author Meherzad Lahewala
 *
 * @since 4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlobalChannelInterceptor {

	/**
	 * An array of patterns against which channel names will be matched.
	 * Since version 5.0 negative patterns are also supported.
	 * A leading '!' negates the pattern match.
	 * Default is "*" (all channels).
	 * @return The pattern.
	 * @see org.springframework.integration.support.utils.PatternMatchUtils#smartMatch(String, String...)
	 */
	String[] patterns() default "*";

	/**
	 * The order of the interceptor. Interceptors with negative order values will be placed before any
	 * explicit interceptors on the channel; interceptors with positive order values will be
	 * placed after explicit interceptors.
	 * @return The order.
	 */
	int order() default 0;

}
