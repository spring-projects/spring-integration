/*
 * Copyright 2002-2013 the original author or authors.
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
 * Indicates that a method is capable of playing the role of a Message Filter.
 * <p>
 * A method annotated with @Filter may accept a parameter of type
 * {@link org.springframework.integration.Message} or of the expected
 * Message payload's type. Any type conversion supported by default or any
 * Converters registered with the "integrationConversionService" bean will be
 * applied to the Message payload if necessary. Header values can also be passed
 * as Message parameters by using the {@link Header @Header} parameter annotation.
 * <p>
 * The return type of the annotated method must be a boolean (or Boolean).
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Filter {

	String inputChannel() default "";

	String outputChannel() default "";

	String[] adviceChain() default {};

	boolean discardWithinAdvice() default true;

}
