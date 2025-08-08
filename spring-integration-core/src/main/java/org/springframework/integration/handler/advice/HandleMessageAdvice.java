/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * The marker {@link MethodInterceptor} interface extension
 * to distinguish advices for some reason.
 *
 * @author Artem Bilan
 * @since 4.3.1
 */
public interface HandleMessageAdvice extends MethodInterceptor {

}
