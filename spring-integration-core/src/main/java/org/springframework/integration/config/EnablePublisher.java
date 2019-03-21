/*
 * Copyright 2014-2019 the original author or authors.
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

import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;

/**
 * Provides the registration for the
 * {@link org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor}
 * to allow the use of the {@link org.springframework.integration.annotation.Publisher} annotation.
 * In addition the {@code default-publisher-channel} name can be configured as
 * the {@link #defaultChannel()} of this annotation.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PublisherRegistrar.class)
public @interface EnablePublisher {

	/**
	 * Alias for the {@link #defaultChannel()} attribute.
	 * The {@code default-publisher-channel} name.
	 * @return the channel bean name.
	 */
	@AliasFor("defaultChannel")
	String value() default "";

	/**
	 * The {@code default-publisher-channel} name.
	 * @return the channel bean name.
	 * @since 5.1.3
	 */
	@AliasFor("value")
	String defaultChannel() default "";

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies.
	 * @return whether proxy target class or not.
	 * @since 5.1.3
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate the order in which the
	 * {@link org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor}
	 * should be applied.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE} in order to run
	 * after all other post-processors, so that it can add an advisor to
	 * existing proxies rather than double-proxy.
	 * @return the order for the bean post-processor.
	 * @since 5.1.3
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
