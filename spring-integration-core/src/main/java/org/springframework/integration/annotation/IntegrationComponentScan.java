/*
 * Copyright 2014-2016 the original author or authors.
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

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.integration.config.IntegrationComponentScanRegistrar;

/**
 * Configures component scanning directives for use with
 * {@link org.springframework.context.annotation.Configuration} classes.
 * <p>
 * Scans for {@link MessagingGateway} on interfaces to create {@code GatewayProxyFactoryBean}s.
 *
 * @author Artem Bilan
 * @since 4.0
 *
 * @see ComponentScan
 * @see MessagingGateway
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(IntegrationComponentScanRegistrar.class)
public @interface IntegrationComponentScan {

	/**
	 * Alias for the {@link #basePackages()} attribute.
	 * Allows for more concise annotation declarations e.g.:
	 * {@code @IntegrationComponentScan("org.my.pkg")} instead of
	 * {@code @IntegrationComponentScan(basePackages="org.my.pkg")}.
	 * @return the array of 'basePackages'.
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components.
	 * The {@link #value()} is an alias for (and mutually exclusive with) this attribute.
	 * Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 * @return the array of 'basePackages'.
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages
	 * to scan for annotated components. The package of each class specified will be scanned.
	 * Consider creating a special no-op marker class or interface in each package
	 * that serves no purpose other than being referenced by this attribute.
	 * @return the array of 'basePackageClasses'.
	 */
	Class<?>[] basePackageClasses() default {};

}
