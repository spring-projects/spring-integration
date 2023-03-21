/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.SimpleSequenceSizeReleaseStrategy;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class AggregatorAnnotationTests {

	@Test
	public void testAnnotationWithDefaultSettings() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {"classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml"});
		final String endpointName = "endpointWithDefaultAnnotation";
		MessageHandler aggregator = this.getAggregator(context, endpointName);
		assertThat(getPropertyValue(aggregator, "releaseStrategy") instanceof SimpleSequenceSizeReleaseStrategy)
				.isTrue();
		assertThat(getPropertyValue(aggregator, "outputChannel")).isNull();
		assertThat(getPropertyValue(aggregator, "messagingTemplate.sendTimeout")).isEqualTo(30000L);
		assertThat(getPropertyValue(aggregator, "sendPartialResultOnExpiry")).isEqualTo(false);
		context.close();
	}

	@Test
	public void testAnnotationWithCustomSettings() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {"classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml"});
		final String endpointName = "endpointWithCustomizedAnnotation";
		MessageHandler aggregator = this.getAggregator(context, endpointName);
		assertThat(getPropertyValue(aggregator, "releaseStrategy") instanceof SimpleSequenceSizeReleaseStrategy)
				.isTrue();
		assertThat(getPropertyValue(aggregator, "outputChannelName")).isEqualTo("outputChannel");
		assertThat(getPropertyValue(aggregator, "discardChannelName")).isEqualTo("discardChannel");
		assertThat(getPropertyValue(aggregator, "messagingTemplate.sendTimeout")).isEqualTo(98765432L);
		assertThat(getPropertyValue(aggregator, "sendPartialResultOnExpiry")).isEqualTo(true);
		context.close();
	}

	@Test
	public void testAnnotationWithCustomReleaseStrategy() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {"classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml"});
		final String endpointName = "endpointWithDefaultAnnotationAndCustomReleaseStrategy";
		MessageHandler aggregator = this.getAggregator(context, endpointName);
		Object releaseStrategy = getPropertyValue(aggregator, "releaseStrategy");
		assertThat(releaseStrategy instanceof MethodInvokingReleaseStrategy).isTrue();
		MethodInvokingReleaseStrategy releaseStrategyAdapter = (MethodInvokingReleaseStrategy) releaseStrategy;
		Object handlerMethods = new DirectFieldAccessor(releaseStrategyAdapter)
				.getPropertyValue("adapter.delegate.handlerMethods");
		assertThat(handlerMethods).isNull();
		Object handlerMethod = new DirectFieldAccessor(releaseStrategyAdapter)
				.getPropertyValue("adapter.delegate.handlerMethod");
		assertThat(handlerMethod.toString().contains("completionChecker")).isTrue();
		context.close();
	}

	@Test
	public void testAnnotationWithCustomCorrelationStrategy() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {"classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml"});
		final String endpointName = "endpointWithCorrelationStrategy";
		MessageHandler aggregator = this.getAggregator(context, endpointName);
		Object correlationStrategy = getPropertyValue(aggregator, "correlationStrategy");
		assertThat(correlationStrategy instanceof MethodInvokingCorrelationStrategy).isTrue();
		MethodInvokingCorrelationStrategy releaseStrategyAdapter = (MethodInvokingCorrelationStrategy) correlationStrategy;
		DirectFieldAccessor processorAccessor =
				new DirectFieldAccessor(TestUtils.getPropertyValue(releaseStrategyAdapter, "processor.delegate"));
		Object targetObject = processorAccessor.getPropertyValue("targetObject");
		assertThat(targetObject).isSameAs(context.getBean(endpointName));
		assertThat(processorAccessor.getPropertyValue("handlerMethods")).isNull();
		Object handlerMethod = processorAccessor.getPropertyValue("handlerMethod");
		assertThat(handlerMethod).isNotNull();
		DirectFieldAccessor handlerMethodAccessor = new DirectFieldAccessor(handlerMethod);
		Method completionCheckerMethod = (Method) handlerMethodAccessor.getPropertyValue("method");
		assertThat(completionCheckerMethod.getName()).isEqualTo("correlate");
		context.close();
	}

	private MessageHandler getAggregator(ApplicationContext context, final String endpointName) {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean(endpointName
				+ ".aggregatingMethod.aggregator");
		return TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class);
	}

}
