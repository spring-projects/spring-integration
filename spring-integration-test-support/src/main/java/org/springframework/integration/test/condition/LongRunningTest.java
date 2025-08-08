/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.test.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.junit.jupiter.EnabledIf;

/**
 * JUnit Jupiter condition to prevent long running tests from running on every build;
 * set environment variable {@code RUN_LONG_INTEGRATION_TESTS} on a CI nightly build to ensure coverage.
 *
 * @author Artem Bilan
 *
 * @since 5.1
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIf("#{environment['RUN_LONG_INTEGRATION_TESTS'] == 'true'}")
public @interface LongRunningTest {

}
