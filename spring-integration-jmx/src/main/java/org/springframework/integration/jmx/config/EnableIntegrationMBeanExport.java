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

package org.springframework.integration.jmx.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * Enables default exporting for Spring Integration components in an existing application, as
 * well as well all {@code @ManagedResource} annotated beans.
 *
 * <p>The resulting {@link org.springframework.integration.monitor.IntegrationMBeanExporter}
 * bean is defined under the name {@code integrationMbeanExporter}. Alternatively, consider defining a
 * custom {@link org.springframework.integration.monitor.IntegrationMBeanExporter} bean explicitly.
 *
 * <p>This annotation is modeled after and functionally equivalent to Spring Integration XML's
 * {@code <int-jmx:mbean-export/>} element.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(IntegrationMBeanExportConfiguration.class)
public @interface EnableIntegrationMBeanExport {

	/**
	 * The default domain to use when generating JMX ObjectNames.
	 * Supports property placeholders (e.g. {@code ${project.domain}}).
	 * @return the domain.
	 */
	String defaultDomain() default "";

	/**
	 * The bean name of the MBeanServer to which MBeans should be exported. Default is to
	 * use the platform's default MBeanServer.
	 * Supports property placeholders (e.g. {@code ${project.mbeanServer}})
	 * and SpEL expression (e.g. {@code #{mbeanServer}}).
	 * @return the server.
	 */
	String server() default "";

	/**
	 * The policy to use when attempting to register an MBean under an
	 * {@link javax.management.ObjectName} that already exists. Defaults to
	 * {@link org.springframework.jmx.support.RegistrationPolicy#FAIL_ON_EXISTING}.
	 * @return the registration policy.
	 */
	RegistrationPolicy registration() default RegistrationPolicy.FAIL_ON_EXISTING;

	/**
	 * List of simple patterns for component names to register (defaults to '*').
	 * The pattern is applied to all components before they are registered, looking for a match on
	 * the 'name' property of the ObjectName.  A MessageChannel and a MessageHandler (for instance)
	 * can share a name because they have a different type, so in that case they would either both
	 * be included or both excluded.
	 * Supports property placeholders (e.g. {@code ${managed.components}}). Can be applied for each element.
	 * @return the patterns.
	 */
	String[] managedComponents() default "*";

}
