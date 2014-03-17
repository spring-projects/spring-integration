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

import org.springframework.context.annotation.Import;

/**
 * Provides the registration for the {@link org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor}
 * to allow the use of the {@link org.springframework.integration.annotation.Publisher} annotation.
 * In addition the {@code default-publisher-channel} name has to be configured as the {@code value} of this annotation.
 * <p>
 * Note: the {@link org.springframework.integration.annotation.Publisher} annotation is enabled by default via
 * {@link EnableIntegration} processing, but there is no hook to configure the {@code default-publisher-channel}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PublisherRegistrar.class)
public @interface EnablePublisher {

	/**
	 * @return the {@code default-publisher-channel} name.
	 */
	String value();
}
