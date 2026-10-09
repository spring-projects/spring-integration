/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.ip.udp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test classes annotated with this will check whether the system supports multicast or not.
 * If it is not supported, tests will be skipped.
 *
 * @author Jiandong Ma
 *
 * @since 6.5.0
 */
@ExtendWith(MulticastCondition.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Multicast {

	/**
	 * The hostname.
	 * @return the hostname.
	 */
	String group() default MulticastCondition.DEFAULT_GROUP;
}
