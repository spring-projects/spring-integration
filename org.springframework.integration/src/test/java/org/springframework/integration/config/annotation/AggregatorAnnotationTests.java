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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import static org.springframework.integration.util.TestUtils.getPropertyValue;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.aggregator.AbstractMessageAggregator;
import org.springframework.integration.aggregator.CompletionStrategyAdapter;
import org.springframework.integration.aggregator.SequenceSizeCompletionStrategy;
import org.springframework.integration.aggregator.CorrelationStrategyAdapter;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.HandlerMethodResolver;
import org.springframework.integration.handler.StaticHandlerMethodResolver;

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
		AbstractMessageAggregator aggregator = this.getAggregator(context, endpointName);
		assertTrue(getPropertyValue(aggregator, "completionStrategy") instanceof SequenceSizeCompletionStrategy);
		assertNull(getPropertyValue(aggregator, "outputChannel"));
		assertNull(getPropertyValue(aggregator, "discardChannel"));
		assertEquals(AbstractMessageAggregator.DEFAULT_SEND_TIMEOUT,
				getPropertyValue(aggregator, "channelTemplate.sendTimeout"));
		assertEquals(AbstractMessageAggregator.DEFAULT_TIMEOUT, getPropertyValue(aggregator, "timeout"));
		assertEquals(false, getPropertyValue(aggregator, "sendPartialResultOnTimeout"));
		assertEquals(AbstractMessageAggregator.DEFAULT_REAPER_INTERVAL,
				getPropertyValue(aggregator, "reaperInterval"));
		assertEquals(AbstractMessageAggregator.DEFAULT_TRACKED_CORRRELATION_ID_CAPACITY,
				getPropertyValue(aggregator, "trackedCorrelationIdCapacity"));
	}

	@Test
	public void testAnnotationWithCustomSettings() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithCustomizedAnnotation";
		AbstractMessageAggregator aggregator = this.getAggregator(context, endpointName);
		assertTrue(getPropertyValue(aggregator, "completionStrategy")
				instanceof SequenceSizeCompletionStrategy);
		ChannelResolver channelResolver = new BeanFactoryChannelResolver(context);
		assertEquals(channelResolver.resolveChannelName("outputChannel"),
				getPropertyValue(aggregator, "outputChannel"));
		assertEquals(channelResolver.resolveChannelName("discardChannel"),
				getPropertyValue(aggregator, "discardChannel"));
		assertEquals(98765432l, getPropertyValue(aggregator, "channelTemplate.sendTimeout"));
		assertEquals(4567890l, getPropertyValue(aggregator, "timeout"));
		assertEquals(true, getPropertyValue(aggregator, "sendPartialResultOnTimeout"));
		assertEquals(1234l, getPropertyValue(aggregator, "reaperInterval"));
		assertEquals(42, getPropertyValue(aggregator, "trackedCorrelationIdCapacity"));
	}

	@Test
	public void testAnnotationWithCustomCompletionStrategy() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithDefaultAnnotationAndCustomCompletionStrategy";
		AbstractMessageAggregator aggregator = this.getAggregator(context, endpointName);
		Object completionStrategy = getPropertyValue(aggregator, "completionStrategy");
		Assert.assertTrue(completionStrategy instanceof CompletionStrategyAdapter);
		CompletionStrategyAdapter completionStrategyAdapter = (CompletionStrategyAdapter) completionStrategy;
		DirectFieldAccessor invokerAccessor = new DirectFieldAccessor(
				new DirectFieldAccessor(completionStrategyAdapter).getPropertyValue("invoker"));
		Object targetObject = invokerAccessor.getPropertyValue("object");
		assertSame(context.getBean(endpointName), targetObject);
		Method completionCheckerMethod = (Method) invokerAccessor.getPropertyValue("method");
		assertEquals("completionChecker", completionCheckerMethod.getName());
	}

    @Test
    public void testAnnotationWithCustomCorrelationStrategy() throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
        final String endpointName = "endpointWithCorrelationStrategy";
        AbstractMessageAggregator aggregator = this.getAggregator(context, endpointName);
        Object correlationStrategy = getPropertyValue(aggregator, "correlationStrategy");
        Assert.assertTrue(correlationStrategy instanceof CorrelationStrategyAdapter);
        CorrelationStrategyAdapter completionStrategyAdapter = (CorrelationStrategyAdapter) correlationStrategy;
        DirectFieldAccessor invokerAccessor = new DirectFieldAccessor(
                new DirectFieldAccessor(completionStrategyAdapter).getPropertyValue("invoker"));
        Object targetObject = invokerAccessor.getPropertyValue("object");
        assertSame(context.getBean(endpointName), targetObject);
        HandlerMethodResolver completionCheckerMethodResolver = (HandlerMethodResolver) invokerAccessor.getPropertyValue("methodResolver");
        assertTrue(completionCheckerMethodResolver instanceof StaticHandlerMethodResolver);
        DirectFieldAccessor resolverAccessor = new DirectFieldAccessor(completionCheckerMethodResolver);
        Method completionCheckerMethod = (Method) resolverAccessor.getPropertyValue("method");
		assertEquals("correlate", completionCheckerMethod.getName());                
    }



	private AbstractMessageAggregator getAggregator(ApplicationContext context, final String endpointName) {
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean(
				endpointName + ".aggregatingMethod.aggregator");
		return (AbstractMessageAggregator) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
	}

}
