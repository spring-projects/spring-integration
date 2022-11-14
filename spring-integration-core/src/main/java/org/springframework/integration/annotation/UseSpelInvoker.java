/*
 * Copyright 2017-2022 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that a POJO handler method ({@code @ServiceActivator, @Transformer, } etc.,
 * or such methods invoked from XML definitions) should be invoked using SpEL.
 * <p>In versions prior to 5.0, such methods were always invoked using SpEL. In 5.0, the
 * framework switched to using
 * {@link org.springframework.messaging.handler.invocation.InvocableHandlerMethod} instead
 * which is generally more efficient than (interpreted) SpEL.
 * <p>There may be some unanticipated corner case where it is necessary to revert to using
 * SpEL. Also, for very high performance requirements, you may wish to consider using
 * compiled SpEL which is often the fastest solution (when the expression is compilable).
 * <p>Applying this annotation to those methods will cause SpEL to be used for the
 * invocation. An optional {@code compilerMode} property (aliased to value) is also provided.
 *
 * @author Gary Russell
 * @since 5.0
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface UseSpelInvoker {

	/**
	 * Specify that the annotated method (or methods in the annotated class) will be
	 * invoked using SpEL instead of an
	 * {@link org.springframework.messaging.handler.invocation.InvocableHandlerMethod}
	 * with the specified compilerMode. If left empty, the default runtime compiler
	 * mode will be used. Must evaluate to a String containing a valid compiler mode.
	 * @return The compilerMode.
	 * @see org.springframework.expression.spel.SpelCompilerMode
	 */
	@AliasFor("compilerMode")
	String value() default "";

	/**
	 * Specify that the annotated method (or methods in the annotated class) will be
	 * invoked using SpEL instead of an
	 * {@link org.springframework.messaging.handler.invocation.InvocableHandlerMethod}
	 * with the specified compilerMode. If left empty, the default runtime compiler
	 * mode will be used. Must evaluate to a String containing a valid compiler mode.
	 * @return The compilerMode.
	 * @see org.springframework.expression.spel.SpelCompilerMode
	 */
	@AliasFor("value")
	String compilerMode() default "";

}
