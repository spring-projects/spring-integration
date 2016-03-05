/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.security.channel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to be applied for the {@link org.springframework.messaging.MessageChannel} bean definition
 * from JavaConfig - on {@code @Bean} method level.
 * <p>
 * Applies the {@link ChannelSecurityInterceptor}(s) using provided {@link #interceptor()} bean name(s).
 * <p>
 * The {@link #sendAccess()} and {@link #receiveAccess()} policies are populated to the
 * {@link ChannelSecurityInterceptor}(s) from the {@code ChannelSecurityInterceptorBeanPostProcessor}.
 *
 * @author Artem Bilan
 * @since 4.2
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SecuredChannel {

	String[] interceptor();

	String[] sendAccess() default {};

	String[] receiveAccess() default {};

}
