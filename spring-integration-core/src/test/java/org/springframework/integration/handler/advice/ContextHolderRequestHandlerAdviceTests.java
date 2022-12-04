/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
