/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class ContextHolderRequestHandlerAdviceTests {

	@Test
	void contextHolderRequestHandlerAdviceInAction() {
		AtomicReference<Object> context = new AtomicReference<>();

		AtomicReference<Object> valueFromHandler = new AtomicReference<>();
		MessageHandler testHandler = message -> valueFromHandler.set(context.get());

		String testContextValue = "test data";

		ContextHolderRequestHandlerAdvice contextHolderRequestHandlerAdvice =
				new ContextHolderRequestHandlerAdvice(m -> testContextValue, context::set, () -> context.set(null));

		ProxyFactoryBean fb = new ProxyFactoryBean();
		fb.setTarget(testHandler);
		fb.addAdvice(contextHolderRequestHandlerAdvice);
		testHandler = (MessageHandler) fb.getObject();

		testHandler.handleMessage(new GenericMessage<>(""));

		assertThat(valueFromHandler.get()).isEqualTo(testContextValue);
		assertThat(context.get()).isNull();
	}

}
