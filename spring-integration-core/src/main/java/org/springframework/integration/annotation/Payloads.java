/*
 * Copyright 2002-2014 the original author or authors.
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

/**
 * This annotation marks a method parameter as being a list of message payloads, for POJO handlers that deal with lists
 * of messages (e.g. aggregators and release strategies).
 * <p>
 * Example:
 * {@code void foo(@Payloads("city.name") List<String> cityName)} - will map the value of the 'name' property of the 'city'
 * property of all the payload objects in the input list.
 *
 * @author Dave Syer
 * @since 2.0
 */
@Target( { ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Payloads {

	/**
	 * @return The expression for matching against nested properties of the payloads.
	 */
	String value() default "";

}
