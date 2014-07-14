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

package org.springframework.integration.ws.config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.ws.MarshallingWebServiceOutboundGateway;
import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
public class WebServiceOutboundGatewayParserTests {

	private static volatile int adviceCalled;

	@Test
	public void simpleGatewayWithReplyChannel() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithReplyChannel");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		Object expected = context.getBean("outputChannel");
		assertEquals(expected, accessor.getPropertyValue("outputChannel"));
		Assert.assertEquals(Boolean.FALSE, accessor.getPropertyValue("requiresReply"));

		@SuppressWarnings("unchecked")
		List<String> requestHeaders = TestUtils.getPropertyValue(endpoint, "handler.headerMapper.requestHeaderNames", List.class);
		@SuppressWarnings("unchecked")
		List<String> replyHeaders = TestUtils.getPropertyValue(endpoint, "handler.headerMapper.replyHeaderNames", List.class);
		assertEquals(1, requestHeaders.size());
		assertEquals(1, replyHeaders.size());
		assertTrue(requestHeaders.contains("testRequest"));
		assertTrue(replyHeaders.contains("testReply"));

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout", Long.class);
		assertEquals(Long.valueOf(777), sendTimeout);
	}

	@Test
	public void simpleGatewayWithIgnoreEmptyResponseTrueByDefault() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithReplyChannel");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("ignoreEmptyResponses"));
		Assert.assertEquals(Boolean.FALSE, accessor.getPropertyValue("requiresReply"));
	}

	@Test
	public void simpleGatewayWithIgnoreEmptyResponses() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithIgnoreEmptyResponsesFalseAndRequiresReplyTrue");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.FALSE, accessor.getPropertyValue("ignoreEmptyResponses"));
		Assert.assertEquals(Boolean.TRUE, accessor.getPropertyValue("requiresReply"));
	}

	@Test
	public void simpleGatewayWithDefaultSourceExtractor() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithDefaultSourceExtractor");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals("DefaultSourceExtractor", accessor.getPropertyValue("sourceExtractor").getClass().getSimpleName());
	}

	@Test
	public void simpleGatewayWithCustomSourceExtractor() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomSourceExtractor");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		SourceExtractor<?> sourceExtractor = (SourceExtractor<?>) context.getBean("sourceExtractor");
		assertEquals(sourceExtractor, accessor.getPropertyValue("sourceExtractor"));
	}

	@Test
	public void simpleGatewayWithCustomRequestCallback() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomRequestCallback");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		WebServiceMessageCallback callback = (WebServiceMessageCallback) context.getBean("requestCallback");
		assertEquals(callback, accessor.getPropertyValue("requestCallback"));
	}

	@Test
	public void simpleGatewayWithCustomMessageFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomMessageFactory");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageFactory factory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(factory, accessor.getPropertyValue("messageFactory"));
	}

	@Test
	public void simpleGatewayWithCustomSourceExtractorAndMessageFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomSourceExtractorAndMessageFactory");
		SourceExtractor<?> sourceExtractor = (SourceExtractor<?>) context.getBean("sourceExtractor");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(sourceExtractor, accessor.getPropertyValue("sourceExtractor"));
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageFactory factory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(factory, accessor.getPropertyValue("messageFactory"));
	}

	@Test
	public void simpleGatewayWithCustomFaultMessageResolver() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomFaultMessageResolver");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		FaultMessageResolver resolver = (FaultMessageResolver) context.getBean("faultMessageResolver");
		assertEquals(resolver, accessor.getPropertyValue("faultMessageResolver"));
	}


	@Test
	public void simpleGatewayWithCustomMessageSender() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomMessageSender");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageSender messageSender = (WebServiceMessageSender) context.getBean("messageSender");
		assertEquals(messageSender, ((WebServiceMessageSender[])accessor.getPropertyValue("messageSenders"))[0]);
	}
	@Test
	public void simpleGatewayWithCustomMessageSenderList() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomMessageSenderList");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageSender messageSender = (WebServiceMessageSender) context.getBean("messageSender");
		assertEquals(messageSender, ((WebServiceMessageSender[])accessor.getPropertyValue("messageSenders"))[0]);
		assertEquals("Wrong number of message senders ",
				2 , ((WebServiceMessageSender[])accessor.getPropertyValue("messageSenders")).length);
	}

	@Test
	public void simpleGatewayWithCustomInterceptor() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomInterceptor");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		ClientInterceptor interceptor = context.getBean("interceptor", ClientInterceptor.class);
		assertEquals(interceptor, ((ClientInterceptor[]) accessor.getPropertyValue("interceptors"))[0]);
	}
	@Test
	public void simpleGatewayWithCustomInterceptorList() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomInterceptorList");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		ClientInterceptor interceptor = context.getBean("interceptor", ClientInterceptor.class);
		assertEquals(interceptor, ((ClientInterceptor[]) accessor.getPropertyValue("interceptors"))[0]);
		assertEquals("Wrong number of interceptors ",
				2 , ((ClientInterceptor[]) accessor.getPropertyValue("interceptors")).length);
	}

	@Test
	public void simpleGatewayWithPoller() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithPoller");
		assertEquals(PollingConsumer.class, endpoint.getClass());
		Object triggerObject = new DirectFieldAccessor(endpoint).getPropertyValue("trigger");
		assertEquals(PeriodicTrigger.class, triggerObject.getClass());
		PeriodicTrigger trigger = (PeriodicTrigger) triggerObject;
		DirectFieldAccessor accessor = new DirectFieldAccessor(trigger);
		assertEquals("PeriodicTrigger had wrong period",
				5000, ((Long)accessor.getPropertyValue("period")).longValue());
	}

	@Test
	public void simpleGatewayWithOrder() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithOrderAndAutoStartupFalse");
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(99, new DirectFieldAccessor(gateway).getPropertyValue("order"));
	}

	@Test
	public void simpleGatewayWithStartupFalse() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithOrderAndAutoStartupFalse");
		assertEquals(Boolean.FALSE, new DirectFieldAccessor(endpoint).getPropertyValue("autoStartup"));
	}

	@Test
	public void marshallingGatewayWithAllInOneMarshaller() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithAllInOneMarshaller");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MarshallingWebServiceOutboundGateway gateway = (MarshallingWebServiceOutboundGateway) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Marshaller marshaller = (Marshaller) context.getBean("marshallerAndUnmarshaller");
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "marshaller", Marshaller.class));
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "unmarshaller", Unmarshaller.class));
	}

	@Test
	public void marshallingGatewayWithSeparateMarshallerAndUnmarshaller() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithSeparateMarshallerAndUnmarshaller");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MarshallingWebServiceOutboundGateway gateway = (MarshallingWebServiceOutboundGateway) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Marshaller marshaller = (Marshaller) context.getBean("marshaller");
		Unmarshaller unmarshaller = (Unmarshaller) context.getBean("unmarshaller");
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "marshaller", Marshaller.class));
		assertEquals(unmarshaller, TestUtils.getPropertyValue(gateway, "unmarshaller", Unmarshaller.class));
	}

	@Test
	public void marshallingGatewayWithCustomRequestCallback() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomRequestCallback");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(MarshallingWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		WebServiceMessageCallback callback = (WebServiceMessageCallback) context.getBean("requestCallback");
		assertEquals(callback, accessor.getPropertyValue("requestCallback"));
	}

	@Test
	public void marshallingGatewayWithAllInOneMarshallerAndMessageFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithAllInOneMarshallerAndMessageFactory");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MarshallingWebServiceOutboundGateway gateway = (MarshallingWebServiceOutboundGateway) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Marshaller marshaller = (Marshaller) context.getBean("marshallerAndUnmarshaller");
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "marshaller", Marshaller.class));
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "unmarshaller", Unmarshaller.class));

		WebServiceMessageFactory messageFactory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(messageFactory, TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageFactory"));
	}

	@Test
	public void marshallingGatewayWithSeparateMarshallerAndUnmarshallerAndMessageFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithSeparateMarshallerAndUnmarshallerAndMessageFactory");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MarshallingWebServiceOutboundGateway gateway = (MarshallingWebServiceOutboundGateway) new DirectFieldAccessor(endpoint).getPropertyValue("handler");

		Marshaller marshaller = (Marshaller) context.getBean("marshaller");
		Unmarshaller unmarshaller = (Unmarshaller) context.getBean("unmarshaller");
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "marshaller", Marshaller.class));
		assertEquals(unmarshaller, TestUtils.getPropertyValue(gateway, "unmarshaller", Unmarshaller.class));
		WebServiceMessageFactory messageFactory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(messageFactory, TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageFactory"));
	}

	@Test
	public void simpleGatewayWithDestinationProvider() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithDestinationProvider");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		StubDestinationProvider stubProvider = (StubDestinationProvider) context.getBean("destinationProvider");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals("Wrong DestinationProvider", stubProvider, accessor.getPropertyValue("destinationProvider"));
		assertNull(accessor.getPropertyValue("uri"));
		Object destinationProviderObject = new DirectFieldAccessor(
				accessor.getPropertyValue("webServiceTemplate")).getPropertyValue("destinationProvider");
		assertEquals("Wrong DestinationProvider", stubProvider,destinationProviderObject);
	}

	@Test
	public void advised() {
		adviceCalled = 0;
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithAdvice");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void testInt2718AdvisedInsideAChain() {
		adviceCalled = 0;
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		MessageChannel channel = context.getBean("gatewayWithAdviceInsideAChain", MessageChannel.class);
		channel.send(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void jmsUri() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithJmsUri");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class);
		assertNull(TestUtils.getPropertyValue(handler, "destinationProvider"));
		assertFalse(TestUtils.getPropertyValue(handler, "encodeUri", Boolean.class));

		WebServiceTemplate webServiceTemplate = TestUtils.getPropertyValue(handler, "webServiceTemplate",
				WebServiceTemplate.class);
		webServiceTemplate = spy(webServiceTemplate);

		doReturn(null).when(webServiceTemplate).sendAndReceive(anyString(),
				any(WebServiceMessageCallback.class),
				any(WebServiceMessageExtractor.class));

		new DirectFieldAccessor(handler).setPropertyValue("webServiceTemplate", webServiceTemplate);

		handler.handleMessage(new GenericMessage<String>("foo"));

		verify(webServiceTemplate).sendAndReceive(eq("jms:wsQueue"),
				any(WebServiceMessageCallback.class),
				any(WebServiceMessageExtractor.class));
	}

    @Test(expected = BeanDefinitionParsingException.class)
    public void invalidGatewayWithBothUriAndDestinationProvider() {
    	new ClassPathXmlApplicationContext("invalidGatewayWithBothUriAndDestinationProvider.xml", this.getClass());
    }

    @Test(expected = BeanDefinitionParsingException.class)
    public void invalidGatewayWithNeitherUriNorDestinationProvider() {
    	new ClassPathXmlApplicationContext("invalidGatewayWithNeitherUriNorDestinationProvider.xml", this.getClass());
    }

    public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

    }
}
