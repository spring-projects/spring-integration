/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate endpoints to assign them to a role. Such endpoints can be started/stopped as
 * a group. See {@code SmartLifecycleRoleController}.
 *
 * @author Gary Russell
 *
 * @since 4.2
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Role {

	/**
	 * @return the role for this endpoint. See {@code SmartLifecycleRoleController}.
	 */
	String value() default "";

}
