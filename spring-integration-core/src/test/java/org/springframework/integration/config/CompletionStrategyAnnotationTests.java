package org.springframework.integration.config;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.ConcurrentHandler;
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
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:/org/springframework/integration/config/testInvalidCompletionStrategyAnnotation.xml" });
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
