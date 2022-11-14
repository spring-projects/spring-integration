/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation to indicate that a method, or all public methods if applied at class-level,
 * should publish Messages.
 * <p>
 * By default, the Message will be constructed from the return value of the method
 * invocation and sent to a channel specified by the {@link #channel()} attribute.
 * However, a combination of both @Payload and @Header annotations can be used to further
 * manage the message structure. See the reference manual for examples.
 * <p>
 * Note: unlike @Gateway, this annotation is used to generate an AOP Advice for an
 * existing service and its method implementation. The message sending is a side effect
 * of the real method invocation and is invoked after the method returns.
 * The advised method(s) are not aware of the messaging interaction.
 * <p>
 * The XML equivalent is {@code <int:publishing-interceptor>}
 *
 * @author Mark Fisher
 * @author Jeff Maxwell
 *
 * @since 2.0
 *
 * @see org.springframework.integration.aop.MessagePublishingInterceptor
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Publisher {

	/**
	 * Alias for the {@link #channel()} attribute.
	 * @return The name of the Message Channel to which Messages will be published.
	 * @since 5.0.4
	 */
	@AliasFor("channel")
	String value() default "";

	/**
	 * @return The name of the Message Channel to which Messages will be published.
	 */
	@AliasFor("value")
	String channel() default "";

}
