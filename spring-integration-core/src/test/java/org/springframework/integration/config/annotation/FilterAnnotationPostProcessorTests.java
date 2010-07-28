/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.config.annotation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class FilterAnnotationPostProcessorTests {

	private final TestApplicationContext context = TestUtils.createTestApplicationContext();

	private final MessagingAnnotationPostProcessor postProcessor = new MessagingAnnotationPostProcessor();

	private final DirectChannel inputChannel = new DirectChannel();

	private final QueueChannel outputChannel = new QueueChannel();


	@Before
	public void init() {
		context.registerChannel("input", inputChannel);
		context.registerChannel("output", outputChannel);
		postProcessor.setBeanFactory(context.getBeanFactory());
		postProcessor.afterPropertiesSet();
	}


	@Test
	public void filterAnnotationWithBooleanPrimitive() {
		testValidFilter(new TestFilterWithBooleanPrimitive());
	}

	@Test
	public void filterAnnotationWithBooleanWrapperClass() {
		testValidFilter(new TestFilterWithBooleanWrapperClass());
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidMethodWithStringReturnType() {
		Object filter = new TestFilterWithStringReturnType();
		postProcessor.postProcessAfterInitialization(filter, "testFilter");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidMethodWithVoidReturnType() {
		Object filter = new TestFilterWithVoidReturnType();
		postProcessor.postProcessAfterInitialization(filter, "testFilter");
	}


	private void testValidFilter(Object filter) {
		postProcessor.postProcessAfterInitialization(filter, "testFilter");
		context.refresh();
		inputChannel.send(new StringMessage("good"));
		Message<?> passed = outputChannel.receive(0);
		assertNotNull(passed);
		inputChannel.send(new StringMessage("bad"));
		assertNull(outputChannel.receive(0));
		context.stop();
	}


	@MessageEndpoint
	@SuppressWarnings("unused")
	private static class TestFilterWithBooleanPrimitive {

		@Filter(inputChannel="input", outputChannel="output")
		public boolean filter(String s) {
			return !s.contains("bad");
		}
	}


	@MessageEndpoint
	@SuppressWarnings("unused")
	private static class TestFilterWithBooleanWrapperClass {

		@Filter(inputChannel="input", outputChannel="output")
		public Boolean filter(String s) {
			return !s.contains("bad");
		}
	}


	@MessageEndpoint
	@SuppressWarnings("unused")
	private static class TestFilterWithStringReturnType {

		@Filter(inputChannel="input", outputChannel="output")
		public String filter(String s) {
			return s;
		}
	}


	@MessageEndpoint
	@SuppressWarnings("unused")
	private static class TestFilterWithVoidReturnType {

		@Filter(inputChannel="input", outputChannel="output")
		public void filter(String s) {
		}
	}

}
