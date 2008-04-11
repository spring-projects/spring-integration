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

package org.springframework.integration.config;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.CompletionStrategyAdapter;

public class CompletionStrategyAnnotationTests {

	@Test
	public void testAnnotationWithDefaultSettings() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/testAnnotatedAggregator.xml" });
		final String endpointName = "endpointWithDefaultAnnotationAndCustomCompletionStrategy";
		DirectFieldAccessor aggregatingMessageHandlerAccessor = getDirectFieldAccessorForAggregatingHandler(context,
				endpointName);
		Assert.assertTrue(aggregatingMessageHandlerAccessor.getPropertyValue("completionStrategy") instanceof CompletionStrategyAdapter);
		DirectFieldAccessor invokerAccessor = new DirectFieldAccessor(new DirectFieldAccessor(
				aggregatingMessageHandlerAccessor.getPropertyValue("completionStrategy")).getPropertyValue("invoker"));
		Assert.assertSame(context.getBean(endpointName), invokerAccessor.getPropertyValue("object"));
		Assert.assertEquals("completionChecker", invokerAccessor.getPropertyValue("method"));

	}
	
	@Test(expected=BeanCreationException.class)
	public void testInvalidAnnotation() {
		new ClassPathXmlApplicationContext(new String[] {
				"classpath:/org/springframework/integration/config/testInvalidCompletionStrategyAnnotation.xml" });
	}

	@SuppressWarnings("unchecked")
	private DirectFieldAccessor getDirectFieldAccessorForAggregatingHandler(ApplicationContext context,
			final String endpointName) {
		MessageBus messageBus = getMessageBus(context);
		DefaultMessageEndpoint endpoint = (DefaultMessageEndpoint) messageBus
				.lookupEndpoint(endpointName + "-endpoint");
		MessageHandlerChain messageHandlerChain = (MessageHandlerChain) endpoint.getHandler();
		AggregatingMessageHandler aggregatingMessageHandler = (AggregatingMessageHandler) ((List) new DirectFieldAccessor(
				messageHandlerChain).getPropertyValue("handlers")).get(0);
		DirectFieldAccessor aggregatingMessageHandlerAccessor = new DirectFieldAccessor(aggregatingMessageHandler);
		return aggregatingMessageHandlerAccessor;
	}

	private MessageBus getMessageBus(ApplicationContext context) {
		MessageBus messageBus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		return messageBus;
	}
}
