/*
 * Copyright 2002-2008 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.aggregator.AbstractMessageAggregator;
import org.springframework.integration.aggregator.CompletionStrategyAdapter;
import org.springframework.integration.aggregator.SequenceSizeCompletionStrategy;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.config.MessageBusParser;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class AggregatorAnnotationTests {

	@Test
	public void testAnnotationWithDefaultSettings() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithDefaultAnnotation";
		DirectFieldAccessor accessor = getDirectFieldAccessorForAggregatingHandler(context,
				endpointName);
		Assert.assertTrue(accessor.getPropertyValue("completionStrategy") instanceof SequenceSizeCompletionStrategy);
		Assert.assertNull(accessor.getPropertyValue("outputChannel"));
		Assert.assertNull(accessor.getPropertyValue("discardChannel"));
		Assert.assertEquals(AbstractMessageAggregator.DEFAULT_SEND_TIMEOUT, accessor.getPropertyValue("sendTimeout"));
		Assert.assertEquals(AbstractMessageAggregator.DEFAULT_TIMEOUT, accessor.getPropertyValue("timeout"));
		Assert.assertEquals(false, accessor.getPropertyValue("sendPartialResultOnTimeout"));
		Assert.assertEquals(AbstractMessageAggregator.DEFAULT_REAPER_INTERVAL, accessor.getPropertyValue("reaperInterval"));
		Assert.assertEquals(AbstractMessageAggregator.DEFAULT_TRACKED_CORRRELATION_ID_CAPACITY,
				accessor.getPropertyValue("trackedCorrelationIdCapacity"));
	}

	@Test
	public void testAnnotationWithCustomSettings() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithCustomizedAnnotation";
		DirectFieldAccessor accessor = getDirectFieldAccessorForAggregatingHandler(context, endpointName);
		Assert.assertTrue(accessor.getPropertyValue("completionStrategy") instanceof SequenceSizeCompletionStrategy);
		Assert.assertEquals(getMessageBus(context).lookupChannel("outputChannel"), accessor.getPropertyValue("outputChannel"));
		Assert.assertEquals(getMessageBus(context).lookupChannel("discardChannel"), accessor.getPropertyValue("discardChannel"));
		Assert.assertEquals(98765432l, accessor.getPropertyValue("sendTimeout"));
		Assert.assertEquals(4567890l, accessor.getPropertyValue("timeout"));
		Assert.assertEquals(true, accessor.getPropertyValue("sendPartialResultOnTimeout"));
		Assert.assertEquals(1234l, accessor.getPropertyValue("reaperInterval"));
		Assert.assertEquals(42, accessor.getPropertyValue("trackedCorrelationIdCapacity"));
	}

	@Test
	public void testAnnotationWithCustomCompletionStrategy() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithDefaultAnnotationAndCustomCompletionStrategy";
		DirectFieldAccessor aggregatingMessageHandlerAccessor = getDirectFieldAccessorForAggregatingHandler(context, endpointName);
		Object completionStrategy = aggregatingMessageHandlerAccessor.getPropertyValue("completionStrategy");
		Assert.assertTrue(completionStrategy instanceof CompletionStrategyAdapter);
		CompletionStrategyAdapter completionStrategyAdapter = (CompletionStrategyAdapter) completionStrategy;
		DirectFieldAccessor invokerAccessor = new DirectFieldAccessor(
				new DirectFieldAccessor(completionStrategyAdapter).getPropertyValue("invoker"));
		Object targetObject = invokerAccessor.getPropertyValue("object");
		Assert.assertSame(context.getBean(endpointName), targetObject);
		Method completionCheckerMethod = (Method) invokerAccessor.getPropertyValue("method");
		Assert.assertEquals("completionChecker", completionCheckerMethod.getName());
	}


	@SuppressWarnings("unchecked")
	private DirectFieldAccessor getDirectFieldAccessorForAggregatingHandler(ApplicationContext context, final String endpointName) {
		SubscribingConsumerEndpoint endpoint = (SubscribingConsumerEndpoint) context.getBean(
				endpointName + ".aggregatingMethod.aggregator");
		return new DirectFieldAccessor(new DirectFieldAccessor(endpoint).getPropertyValue("consumer"));
	}

	private MessageBus getMessageBus(ApplicationContext context) {
		return (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
	}

}
