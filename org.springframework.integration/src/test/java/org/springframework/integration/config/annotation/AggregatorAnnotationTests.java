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

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.config.MessageBusParser;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.CompletionStrategyAdapter;
import org.springframework.integration.router.SequenceSizeCompletionStrategy;

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
		DirectFieldAccessor aggregatingMessageHandlerAccessor = getDirectFieldAccessorForAggregatingHandler(context,
				endpointName);
		Assert.assertTrue(aggregatingMessageHandlerAccessor.getPropertyValue("completionStrategy") instanceof SequenceSizeCompletionStrategy);
		Assert.assertNull(aggregatingMessageHandlerAccessor.getPropertyValue("defaultReplyChannel"));
		Assert.assertNull(aggregatingMessageHandlerAccessor.getPropertyValue("discardChannel"));
		Assert.assertEquals(AggregatingMessageHandler.DEFAULT_SEND_TIMEOUT, aggregatingMessageHandlerAccessor
				.getPropertyValue("sendTimeout"));
		Assert.assertEquals(AggregatingMessageHandler.DEFAULT_TIMEOUT, aggregatingMessageHandlerAccessor
				.getPropertyValue("timeout"));
		Assert.assertEquals(false, aggregatingMessageHandlerAccessor.getPropertyValue("sendPartialResultOnTimeout"));
		Assert.assertEquals(AggregatingMessageHandler.DEFAULT_REAPER_INTERVAL, aggregatingMessageHandlerAccessor
				.getPropertyValue("reaperInterval"));
		Assert.assertEquals(AggregatingMessageHandler.DEFAULT_TRACKED_CORRRELATION_ID_CAPACITY,
				aggregatingMessageHandlerAccessor.getPropertyValue("trackedCorrelationIdCapacity"));
	}

	@Test
	public void testAnnotationWithCustomSettings() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithCustomizedAnnotation";
		DirectFieldAccessor aggregatingMessageHandlerAccessor = getDirectFieldAccessorForAggregatingHandler(context,
				endpointName);
		Assert.assertTrue(aggregatingMessageHandlerAccessor.getPropertyValue("completionStrategy") instanceof SequenceSizeCompletionStrategy);
		Assert.assertEquals(getMessageBus(context).lookupChannel("replyChannel"), aggregatingMessageHandlerAccessor
				.getPropertyValue("defaultReplyChannel"));
		Assert.assertEquals(getMessageBus(context).lookupChannel("discardChannel"), aggregatingMessageHandlerAccessor
				.getPropertyValue("discardChannel"));
		Assert.assertEquals(98765432l, aggregatingMessageHandlerAccessor
				.getPropertyValue("sendTimeout"));
		Assert.assertEquals(4567890l, aggregatingMessageHandlerAccessor
				.getPropertyValue("timeout"));
		Assert.assertEquals(true, aggregatingMessageHandlerAccessor.getPropertyValue("sendPartialResultOnTimeout"));
		Assert.assertEquals(1234l, aggregatingMessageHandlerAccessor
				.getPropertyValue("reaperInterval"));
		Assert.assertEquals(42,
				aggregatingMessageHandlerAccessor.getPropertyValue("trackedCorrelationIdCapacity"));
	}

	@Test
	public void testAnnotationWithCustomCompletionStrategy() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/annotation/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithDefaultAnnotationAndCustomCompletionStrategy";
		DirectFieldAccessor aggregatingMessageHandlerAccessor = getDirectFieldAccessorForAggregatingHandler(context,
				endpointName);
		Assert.assertTrue(aggregatingMessageHandlerAccessor.getPropertyValue("completionStrategy") instanceof CompletionStrategyAdapter);
		DirectFieldAccessor invokerAccessor = new DirectFieldAccessor(new DirectFieldAccessor(
				aggregatingMessageHandlerAccessor.getPropertyValue("completionStrategy")).getPropertyValue("invoker"));
		Assert.assertSame(((Advised) context.getBean(endpointName)).getTargetSource().getTarget(), invokerAccessor.getPropertyValue("object"));
		Method completionCheckerMethod = (Method) invokerAccessor.getPropertyValue("method");
		Assert.assertEquals("completionChecker", completionCheckerMethod.getName());
	}

	@Test(expected=BeanCreationException.class)
	public void testInvalidCompletionStrategyAnnotation() {
		new ClassPathXmlApplicationContext(new String[] {
				"classpath:/org/springframework/integration/config/annotation/testInvalidCompletionStrategyAnnotation.xml" });
	}


	@SuppressWarnings("unchecked")
	private DirectFieldAccessor getDirectFieldAccessorForAggregatingHandler(ApplicationContext context, final String endpointName) {
		MessageBus messageBus = this.getMessageBus(context);
		HandlerEndpoint endpoint = (HandlerEndpoint) messageBus.lookupEndpoint(endpointName +  ".MessageHandler.endpoint");
		MessageHandler handler = endpoint.getHandler();
		try {
			if (AopUtils.isAopProxy(handler)) {
				DelegatingIntroductionInterceptor interceptor = (DelegatingIntroductionInterceptor)
						((Advised) handler).getAdvisors()[0].getAdvice();
				Object delegate = new DirectFieldAccessor(interceptor).getPropertyValue("delegate");
				return new DirectFieldAccessor(delegate);
			}
		}
		catch (Exception e) {
			// will return the accessor for the handler
		}
		return new DirectFieldAccessor(endpoint.getHandler());
	}

	private MessageBus getMessageBus(ApplicationContext context) {
		return (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
	}

}
