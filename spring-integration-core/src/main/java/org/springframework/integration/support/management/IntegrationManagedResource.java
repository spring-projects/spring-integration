/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.support.management;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Clone of {@link org.springframework.jmx.export.annotation.ManagedResource}
 * limiting beans thus annotated so that they
 * will only be exported by the {@code IntegrationMBeanExporter} and prevented
 * from being exported by other MBeanExporters (if present).
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface IntegrationManagedResource {

	/**
	 * The annotation value is equivalent to the {@code objectName}
	 * attribute, for simple default usage.
	 * @return the value.
	 */
	@AliasFor("objectName")
	String value() default "";

	@AliasFor("value")
	String objectName() default "";

	String description() default "";

	int currencyTimeLimit() default -1;

	boolean log() default false;

	String logFile() default "";

	String persistPolicy() default "";

	int persistPeriod() default -1;

	String persistName() default "";

	String persistLocation() default "";

}
