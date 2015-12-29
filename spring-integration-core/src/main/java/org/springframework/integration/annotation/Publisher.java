/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a method, or all public methods if applied at
 * class-level, should publish Messages.
 * <p>
 * The Message will be constructed from the return value of the method invocation and sent
 * to a channel specified by the channel attribute.
 * To further manage message structure, a combination of both @Payload and @Header annotations
 * can be used as well.
 * <p>
 * Note: unlike @Gateway, this annotation represents typical AOP Advice for existing service
 * and its method implementation.
 * <p>
 * The XML equivalent is {@code <int:publishing-interceptor>}
 *
 * @author Mark Fisher
 * @since 2.0
 * @see org.springframework.integration.aop.MessagePublishingInterceptor
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Publisher {

	/**
	 * @return The name of the Message Channel to which Messages will be published.
	 */
	String channel() default "";

}
