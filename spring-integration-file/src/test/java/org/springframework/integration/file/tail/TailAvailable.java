/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.tail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
@ExtendWith(TailCondition.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TailAvailable {

	/**
	 * The options for the 'tail' command.
	 * @return the options.
	 * @author Jiandong Ma
	 * @since 6.5
	 */
	String options() default "";
}
