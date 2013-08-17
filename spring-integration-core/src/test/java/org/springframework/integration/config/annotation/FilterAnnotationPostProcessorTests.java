/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;

/**
 * @author Mark Fisher
 * @author Gary Russell
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
	public void filterAnnotationWithAdviceDiscardWithin() {
		TestAdvice advice = new TestAdvice();
		context.registerBean("adviceChain", advice);
		testValidFilter(new TestFilterWithAdviceDiscardWithin());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		assertSame(advice, TestUtils.getPropertyValue(endpoint, "handler.adviceChain", List.class).get(0));
		assertTrue(TestUtils.getPropertyValue(endpoint, "handler.postProcessWithinAdvice", Boolean.class));
	}

	@Test
	public void filterAnnotationWithAdviceDiscardWithinTwice() {
		TestAdvice advice = new TestAdvice();
		context.registerBean("adviceChain", advice);
		testValidFilter(new TestFilterWithAdviceDiscardWithinTwice());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		List<?> adviceList = TestUtils.getPropertyValue(endpoint, "handler.adviceChain", List.class);
		assertEquals(2, adviceList.size());
		assertSame(advice, adviceList.get(0));
		assertSame(advice, adviceList.get(1));
		assertTrue(TestUtils.getPropertyValue(endpoint, "handler.postProcessWithinAdvice", Boolean.class));
	}

	@Test
	public void filterAnnotationWithAdviceDiscardWithout() {
		TestAdvice advice = new TestAdvice();
		context.registerBean("adviceChain", advice);
		testValidFilter(new TestFilterWithAdviceDiscardWithout());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		assertSame(advice, TestUtils.getPropertyValue(endpoint, "handler.adviceChain", List.class).get(0));
		assertFalse(TestUtils.getPropertyValue(endpoint, "handler.postProcessWithinAdvice", Boolean.class));
	}

	@Test
	public void filterAnnotationWithAdviceArray() {
		TestAdvice advice = new TestAdvice();
		context.registerBean("adviceChain", new TestAdvice[] {advice});
		testValidFilter(new TestFilterWithAdviceDiscardWithin());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		assertSame(advice, TestUtils.getPropertyValue(endpoint, "handler.adviceChain", List.class).get(0));
		assertTrue(TestUtils.getPropertyValue(endpoint, "handler.postProcessWithinAdvice", Boolean.class));
	}

	@Test
	public void filterAnnotationWithAdviceArrayTwice() {
		TestAdvice advice = new TestAdvice();
		context.registerBean("adviceChain", new TestAdvice[] {advice, advice});
		testValidFilter(new TestFilterWithAdviceDiscardWithinTwice());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		List<?> adviceList = TestUtils.getPropertyValue(endpoint, "handler.adviceChain", List.class);
		assertEquals(4, adviceList.size());
		assertSame(advice, adviceList.get(0));
		assertSame(advice, adviceList.get(1));
		assertSame(advice, adviceList.get(2));
		assertSame(advice, adviceList.get(3));
		assertTrue(TestUtils.getPropertyValue(endpoint, "handler.postProcessWithinAdvice", Boolean.class));
	}

	@Test
	public void filterAnnotationWithAdviceCollection() {
		TestAdvice advice = new TestAdvice();
		context.registerBean("adviceChain", Arrays.asList(new TestAdvice[] {advice}));
		testValidFilter(new TestFilterWithAdviceDiscardWithin());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		assertSame(advice, TestUtils.getPropertyValue(endpoint, "handler.adviceChain", List.class).get(0));
		assertTrue(TestUtils.getPropertyValue(endpoint, "handler.postProcessWithinAdvice", Boolean.class));
	}

	@Test
	public void filterAnnotationWithAdviceCollectionTwice() {
		TestAdvice advice = new TestAdvice();
		context.registerBean("adviceChain", Arrays.asList(new TestAdvice[] {advice, advice}));
		testValidFilter(new TestFilterWithAdviceDiscardWithinTwice());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		List<?> adviceList = TestUtils.getPropertyValue(endpoint, "handler.adviceChain", List.class);
		assertEquals(4, adviceList.size());
		assertSame(advice, adviceList.get(0));
		assertSame(advice, adviceList.get(1));
		assertSame(advice, adviceList.get(2));
		assertSame(advice, adviceList.get(3));
		assertTrue(TestUtils.getPropertyValue(endpoint, "handler.postProcessWithinAdvice", Boolean.class));
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
		inputChannel.send(new GenericMessage<String>("good"));
		Message<?> passed = outputChannel.receive(0);
		assertNotNull(passed);
		inputChannel.send(new GenericMessage<String>("bad"));
		assertNull(outputChannel.receive(0));
		context.stop();
	}


	@MessageEndpoint
	private static class TestFilterWithBooleanPrimitive {

		@Filter(inputChannel="input", outputChannel="output")
		public boolean filter(String s) {
			return !s.contains("bad");
		}
	}

	@MessageEndpoint
	private static class TestFilterWithAdviceDiscardWithin {

		@Filter(inputChannel="input", outputChannel="output", adviceChain="adviceChain")
		public boolean filter(String s) {
			return !s.contains("bad");
		}
	}

	@MessageEndpoint
	private static class TestFilterWithAdviceDiscardWithinTwice {

		@Filter(inputChannel="input", outputChannel="output", adviceChain={"adviceChain", "adviceChain"})
		public boolean filter(String s) {
			return !s.contains("bad");
		}
	}

	@MessageEndpoint
	private static class TestFilterWithAdviceDiscardWithout {

		@Filter(inputChannel="input", outputChannel="output",
				adviceChain="adviceChain", discardWithinAdvice=false)
		public boolean filter(String s) {
			return !s.contains("bad");
		}
	}

	@MessageEndpoint
	private static class TestFilterWithBooleanWrapperClass {

		@Filter(inputChannel="input", outputChannel="output")
		public Boolean filter(String s) {
			return !s.contains("bad");
		}
	}


	@MessageEndpoint
	private static class TestFilterWithStringReturnType {

		@Filter(inputChannel="input", outputChannel="output")
		public String filter(String s) {
			return s;
		}
	}


	@MessageEndpoint
	private static class TestFilterWithVoidReturnType {

		@Filter(inputChannel="input", outputChannel="output")
		public void filter(String s) {
		}
	}

	public static class TestAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			return callback.execute();
		}

	}
}
