/*
 * Copyright 2002-2014 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.SequenceSizeReleaseStrategy;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class AggregatorAnnotationTests {

	@Test
	public void testAnnotationWithDefaultSettings() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithDefaultAnnotation";
		MessageHandler aggregator = this.getAggregator(context, endpointName);
		assertTrue(getPropertyValue(aggregator, "releaseStrategy") instanceof SequenceSizeReleaseStrategy);
		assertNull(getPropertyValue(aggregator, "outputChannel"));
		assertTrue(getPropertyValue(aggregator, "discardChannel") instanceof NullChannel);
		assertEquals(AggregatingMessageHandler.DEFAULT_SEND_TIMEOUT, getPropertyValue(aggregator,
				"messagingTemplate.sendTimeout"));
		assertEquals(false, getPropertyValue(aggregator, "sendPartialResultOnExpiry"));
	}

	@Test
	public void testAnnotationWithCustomSettings() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithCustomizedAnnotation";
		MessageHandler aggregator = this.getAggregator(context, endpointName);
		assertTrue(getPropertyValue(aggregator, "releaseStrategy") instanceof SequenceSizeReleaseStrategy);
		assertEquals("outputChannel", getPropertyValue(aggregator, "outputChannelName"));
		assertEquals("discardChannel", getPropertyValue(aggregator, "discardChannelName"));
		assertEquals(98765432l, getPropertyValue(aggregator, "messagingTemplate.sendTimeout"));
		assertEquals(true, getPropertyValue(aggregator, "sendPartialResultOnExpiry"));
	}

	@Test
	public void testAnnotationWithCustomReleaseStrategy() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithDefaultAnnotationAndCustomReleaseStrategy";
		MessageHandler aggregator = this.getAggregator(context, endpointName);
		Object releaseStrategy = getPropertyValue(aggregator, "releaseStrategy");
		Assert.assertTrue(releaseStrategy instanceof MethodInvokingReleaseStrategy);
		MethodInvokingReleaseStrategy releaseStrategyAdapter = (MethodInvokingReleaseStrategy) releaseStrategy;
		Object handlerMethods = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(releaseStrategyAdapter)
				.getPropertyValue("adapter")).getPropertyValue("delegate")).getPropertyValue("handlerMethods");
		assertNull(handlerMethods);
		Object handlerMethod = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(releaseStrategyAdapter)
				.getPropertyValue("adapter")).getPropertyValue("delegate")).getPropertyValue("handlerMethod");
		assertTrue(handlerMethod.toString().contains("completionChecker"));
	}

	@Test
	public void testAnnotationWithCustomCorrelationStrategy() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithCorrelationStrategy";
		MessageHandler aggregator = this.getAggregator(context, endpointName);
		Object correlationStrategy = getPropertyValue(aggregator, "correlationStrategy");
		Assert.assertTrue(correlationStrategy instanceof MethodInvokingCorrelationStrategy);
		MethodInvokingCorrelationStrategy releaseStrategyAdapter = (MethodInvokingCorrelationStrategy) correlationStrategy;
		DirectFieldAccessor processorAccessor = new DirectFieldAccessor(new DirectFieldAccessor(new DirectFieldAccessor(releaseStrategyAdapter)
				.getPropertyValue("processor")).getPropertyValue("delegate"));
		Object targetObject = processorAccessor.getPropertyValue("targetObject");
		assertSame(context.getBean(endpointName), targetObject);
		assertNull(processorAccessor.getPropertyValue("handlerMethods"));
		Object handlerMethod = processorAccessor.getPropertyValue("handlerMethod");
		assertNotNull(handlerMethod);
		DirectFieldAccessor handlerMethodAccessor = new DirectFieldAccessor(handlerMethod);
		Method completionCheckerMethod = (Method) handlerMethodAccessor.getPropertyValue("method");
		assertEquals("correlate", completionCheckerMethod.getName());
	}

	private MessageHandler getAggregator(ApplicationContext context, final String endpointName) {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean(endpointName
				+ ".aggregatingMethod.aggregator");
		return TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class);
	}

}
