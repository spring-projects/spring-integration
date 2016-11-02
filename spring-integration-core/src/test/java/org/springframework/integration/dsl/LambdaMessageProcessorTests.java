/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.support.GenericMessage;


/**
 * @author Gary Russell
 *
 * @since 5.0
 */
public class LambdaMessageProcessorTests {

	@Test
	@SuppressWarnings("divzero")
	public void testException() {
		try {
			handle((m, h) -> 1 / 0);
			fail("Expected exception");
		}
		catch (Exception e) {
			assertThat(e.getCause(), instanceOf(ArithmeticException.class));
		}
	}

	private void handle(GenericHandler<?> h) {
		LambdaMessageProcessor lmp = new LambdaMessageProcessor(h, String.class);
		lmp.setBeanFactory(mock(BeanFactory.class));
		lmp.processMessage(new GenericMessage<>("foo"));
	}

}
