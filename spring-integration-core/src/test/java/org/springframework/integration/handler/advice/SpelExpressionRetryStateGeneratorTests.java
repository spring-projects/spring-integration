/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.handler.advice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.classify.ClassifierSupport;
import org.springframework.messaging.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.retry.RetryState;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SpelExpressionRetryStateGeneratorTests {

	@Autowired
	private RetryStateGenerator configGenerator;

	private Message<String> message = MessageBuilder.withPayload("Hello, world!")
				.setHeader("foo", "bar")
				.setHeader("trueHeader", true)
				.setHeader("falseHeader", false)
				.build();

	@Test
	public void testBasic() {
		SpelExpressionRetryStateGenerator generator =
			new SpelExpressionRetryStateGenerator("headers['foo']");
		RetryState state = generator.determineRetryState(message);
		assertEquals("bar", state.getKey());
		assertFalse(((DefaultRetryState) state).isForceRefresh());
		assertTrue(state.rollbackFor(new RuntimeException()));
	}

	@Test
	public void testBasicConfig() {
		RetryState state = configGenerator.determineRetryState(message);
		assertEquals("bar", state.getKey());
		assertFalse(((DefaultRetryState) state).isForceRefresh());
		assertTrue(state.rollbackFor(new RuntimeException()));
	}

	@Test
	public void testForceRefreshTrue() {
		SpelExpressionRetryStateGenerator generator =
			new SpelExpressionRetryStateGenerator("headers['foo']", "headers['trueHeader']");
		RetryState state = generator.determineRetryState(message);
		assertEquals("bar", state.getKey());
		assertTrue(((DefaultRetryState) state).isForceRefresh());
		assertTrue(state.rollbackFor(new RuntimeException()));
	}

	@Test
	public void testForceRefreshFalse() {
		SpelExpressionRetryStateGenerator generator =
			new SpelExpressionRetryStateGenerator("headers['foo']", "headers['falseHeader']");
		RetryState state = generator.determineRetryState(message);
		assertEquals("bar", state.getKey());
		assertFalse(((DefaultRetryState) state).isForceRefresh());
		assertTrue(state.rollbackFor(new RuntimeException()));
	}

	@Test
	public void testForceRefreshElvis() {
		SpelExpressionRetryStateGenerator generator =
			new SpelExpressionRetryStateGenerator("headers['foo']", "headers['noHeader']?:true");
		RetryState state = generator.determineRetryState(message);
		assertEquals("bar", state.getKey());
		assertTrue(((DefaultRetryState) state).isForceRefresh());
		assertTrue(state.rollbackFor(new RuntimeException()));
	}

	@Test
	public void testClassifier() {
		SpelExpressionRetryStateGenerator generator =
				new SpelExpressionRetryStateGenerator("headers['foo']");
		generator.setClassifier(new ClassifierSupport<Throwable, Boolean>(false));
		RetryState state = generator.determineRetryState(message);
		assertEquals("bar", state.getKey());
		assertFalse(((DefaultRetryState) state).isForceRefresh());
		assertFalse(state.rollbackFor(new RuntimeException()));
	}
}
