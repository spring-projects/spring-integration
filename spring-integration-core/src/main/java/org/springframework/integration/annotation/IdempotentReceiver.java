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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate a method with the MessagingAnnotation (@code @ServiceActivator, @Router etc.)
 * to apply for the underlying {@link org.springframework.messaging.MessageHandler#handleMessage}
 * the {@link org.springframework.integration.handler.advice.IdempotentReceiverInterceptor} using
 * its id from the {@link #value()}.
 *
 * @author Artem Bilan
 * @since 4.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdempotentReceiver {

	/**
	 * @return the {@link org.springframework.integration.handler.advice.IdempotentReceiverInterceptor}
	 * bean reference.
	 */
	String value();

}
