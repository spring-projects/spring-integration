/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * The main configuration annotation to enable Spring Integration infrastructure:
 * - Registers some built-in beans;
 * - Adds several {@code BeanFactoryPostProcessor}s;
 * - Adds several {@code BeanPostProcessor}s;
 * - Adds annotations processors.
 * <p>
 * Add this annotation to an {@code @Configuration} class to have
 * the imported Spring Integration configuration :
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableIntegration
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyIntegrationConfiguration {
 * }
 * </pre>
 *
 * @author Artem Bilan
 * @since 4.0
 * @see IntegrationRegistrar
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(IntegrationRegistrar.class)
public @interface EnableIntegration {

}
