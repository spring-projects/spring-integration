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

package org.springframework.integration.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation to mark {@link org.springframework.messaging.support.ChannelInterceptor} components
 * to be configured as {@link org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper}
 * with provided {@code patterns}.
 * <p>
 * Can be used on {@code class} level for {@link org.springframework.stereotype.Component} beans
 * and on methods with {@link org.springframework.context.annotation.Bean}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlobalChannelInterceptor {

	String[] patterns() default "*";

	int order() default 0;

}
