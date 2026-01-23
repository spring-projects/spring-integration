/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.integration.config.MessagingAnnotationBeanPostProcessor;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class FilterAnnotationPostProcessorTests {

	private final GenericApplicationContext context = new GenericApplicationContext();

	private final DirectChannel inputChannel = new DirectChannel();

	private final QueueChannel outputChannel = new QueueChannel();

	@BeforeEach
	public void init() {
		new IntegrationRegistrar().registerBeanDefinitions(mock(), this.context.getDefaultListableBeanFactory());
		TestUtils.registerBean("input", inputChannel, this.context);
		TestUtils.registerBean("output", this.outputChannel, this.context);
	}

	@AfterEach
	public void tearDown() {
		this.context.close();
	}

	@Test
	public void filterAnnotationWithBooleanPrimitive() {
		testValidFilter(new TestFilterWithBooleanPrimitive());
	}

	@Test
	public void filterAnnotationWithAdviceDiscardWithin() {
		TestAdvice advice = new TestAdvice();
		TestUtils.registerBean("adviceChain", advice, this.context);
		testValidFilter(new TestFilterWithAdviceDiscardWithin());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		assertThat(TestUtils.<List<?>>getPropertyValue(endpoint, "handler.adviceChain").get(0)).isSameAs(advice);
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.postProcessWithinAdvice")).isTrue();
	}

	@Test
	public void filterAnnotationWithAdviceDiscardWithinTwice() {
		TestAdvice advice1 = new TestAdvice();
		TestAdvice advice2 = new TestAdvice();
		TestUtils.registerBean("adviceChain1", advice1, this.context);
		TestUtils.registerBean("adviceChain2", advice2, this.context);
		testValidFilter(new TestFilterWithAdviceDiscardWithinTwice());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		List<TestAdvice> adviceList = TestUtils.getPropertyValue(endpoint, "handler.adviceChain");
		assertThat(adviceList).hasSize(2)
				.containsExactly(advice1, advice2);
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.postProcessWithinAdvice")).isTrue();
	}

	@Test
	public void filterAnnotationWithAdviceDiscardWithout() {
		TestAdvice advice = new TestAdvice();
		TestUtils.registerBean("adviceChain", advice, this.context);
		testValidFilter(new TestFilterWithAdviceDiscardWithout());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		assertThat(TestUtils.<List<?>>getPropertyValue(endpoint, "handler.adviceChain").get(0)).isSameAs(advice);
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.postProcessWithinAdvice")).isFalse();
	}

	@Test
	public void filterAnnotationWithAdviceArray() {
		TestAdvice advice = new TestAdvice();
		TestUtils.registerBean("adviceChain", new TestAdvice[] {advice}, this.context);
		testValidFilter(new TestFilterWithAdviceDiscardWithin());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		assertThat(TestUtils.<List<?>>getPropertyValue(endpoint, "handler.adviceChain").get(0)).isSameAs(advice);
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.postProcessWithinAdvice")).isTrue();
	}

	@Test
	public void filterAnnotationWithAdviceArrayTwice() {
		TestAdvice advice1 = new TestAdvice();
		TestAdvice advice2 = new TestAdvice();
		TestUtils.registerBean("adviceChain1", new TestAdvice[] {advice1, advice2}, this.context);
		TestAdvice advice3 = new TestAdvice();
		TestAdvice advice4 = new TestAdvice();
		TestUtils.registerBean("adviceChain2", new TestAdvice[] {advice3, advice4}, this.context);
		testValidFilter(new TestFilterWithAdviceDiscardWithinTwice());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		List<TestAdvice> adviceList = TestUtils.getPropertyValue(endpoint, "handler.adviceChain");
		assertThat(adviceList).hasSize(4)
				.containsExactly(advice1, advice2, advice3, advice4);
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.postProcessWithinAdvice")).isTrue();
	}

	@Test
	public void filterAnnotationWithAdviceCollection() {
		TestAdvice advice = new TestAdvice();
		TestUtils.registerBean("adviceChain", Collections.singletonList(advice), this.context);
		testValidFilter(new TestFilterWithAdviceDiscardWithin());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		assertThat(TestUtils.<List<?>>getPropertyValue(endpoint, "handler.adviceChain").get(0)).isSameAs(advice);
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.postProcessWithinAdvice")).isTrue();
	}

	@Test
	public void filterAnnotationWithAdviceCollectionTwice() {
		TestAdvice advice1 = new TestAdvice();
		TestAdvice advice2 = new TestAdvice();
		TestUtils.registerBean("adviceChain1", new TestAdvice[] {advice1, advice2}, this.context);
		TestAdvice advice3 = new TestAdvice();
		TestAdvice advice4 = new TestAdvice();
		TestUtils.registerBean("adviceChain2", new TestAdvice[] {advice3, advice4}, this.context);
		testValidFilter(new TestFilterWithAdviceDiscardWithinTwice());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("testFilter.filter.filter");
		List<TestAdvice> adviceList = TestUtils.getPropertyValue(endpoint, "handler.adviceChain");
		assertThat(adviceList).hasSize(4)
				.containsExactly(advice1, advice2, advice3, advice4);
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.postProcessWithinAdvice")).isTrue();
	}

	@Test
	public void filterAnnotationWithBooleanWrapperClass() {
		testValidFilter(new TestFilterWithBooleanWrapperClass());
	}

	@Test
	public void invalidMethodWithStringReturnType() {
		context.refresh();
		var postProcessor = context.getBean(MessagingAnnotationBeanPostProcessor.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() ->
						postProcessor.postProcessAfterInitialization(
								new TestFilterWithStringReturnType(), "testFilter"));
	}

	@Test
	public void invalidMethodWithVoidReturnType() {
		context.refresh();
		var postProcessor = context.getBean(MessagingAnnotationBeanPostProcessor.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() ->
						postProcessor.postProcessAfterInitialization(new TestFilterWithVoidReturnType(), "testFilter"));
	}

	private void testValidFilter(Object filter) {
		TestUtils.registerBean("testFilter", filter, this.context);
		context.refresh();
		inputChannel.send(new GenericMessage<>("good"));
		Message<?> passed = outputChannel.receive(0);
		assertThat(passed).isNotNull();
		inputChannel.send(new GenericMessage<>("bad"));
		assertThat(outputChannel.receive(0)).isNull();
		context.stop();
	}

	@MessageEndpoint
	public static class TestFilterWithBooleanPrimitive {

		@Filter(inputChannel = "input", outputChannel = "output")
		public boolean filter(String s) {
			return !s.contains("bad");
		}

	}

	@MessageEndpoint
	public static class TestFilterWithAdviceDiscardWithin {

		@Filter(inputChannel = "input", outputChannel = "output", adviceChain = "adviceChain")
		public boolean filter(String s) {
			return !s.contains("bad");
		}

	}

	@MessageEndpoint
	public static class TestFilterWithAdviceDiscardWithinTwice {

		@Filter(inputChannel = "input", outputChannel = "output", adviceChain = {"adviceChain1", "adviceChain2"})
		public boolean filter(String s) {
			return !s.contains("bad");
		}

	}

	@MessageEndpoint
	public static class TestFilterWithAdviceDiscardWithout {

		@Filter(inputChannel = "input", outputChannel = "output",
				adviceChain = "adviceChain", discardWithinAdvice = "false")
		public boolean filter(String s) {
			return !s.contains("bad");
		}

	}

	@MessageEndpoint
	public static class TestFilterWithBooleanWrapperClass {

		@Filter(inputChannel = "input", outputChannel = "output")
		public Boolean filter(String s) {
			return !s.contains("bad");
		}

	}

	@MessageEndpoint
	public static class TestFilterWithStringReturnType {

		@Filter(inputChannel = "input", outputChannel = "output")
		public String filter(String s) {
			return s;
		}

	}

	@MessageEndpoint
	public static class TestFilterWithVoidReturnType {

		@Filter(inputChannel = "input", outputChannel = "output")
		public void filter(String s) {
		}

	}

	public static class TestAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			return callback.execute();
		}

	}

}
