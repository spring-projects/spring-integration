/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.mapping.AbstractHeaderMapper;
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
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithReplyChannel");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		Object expected = context.getBean("outputChannel");
		assertEquals(expected, accessor.getPropertyValue("outputChannel"));
		Assert.assertEquals(Boolean.FALSE, accessor.getPropertyValue("requiresReply"));

		AbstractHeaderMapper.HeaderMatcher requestHeaderMatcher = TestUtils.getPropertyValue(endpoint,
				"handler.headerMapper.requestHeaderMatcher", AbstractHeaderMapper.HeaderMatcher.class);
		assertTrue(requestHeaderMatcher.matchHeader("testRequest"));
		assertFalse(requestHeaderMatcher.matchHeader("testReply"));

		AbstractHeaderMapper.HeaderMatcher replyHeaderMatcher = TestUtils.getPropertyValue(endpoint,
				"handler.headerMapper.replyHeaderMatcher", AbstractHeaderMapper.HeaderMatcher.class);
		assertFalse(replyHeaderMatcher.matchHeader("testRequest"));
		assertTrue(replyHeaderMatcher.matchHeader("testReply"));

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout", Long.class);
		assertEquals(Long.valueOf(777), sendTimeout);
		context.close();
	}

	@Test
	public void simpleGatewayWithIgnoreEmptyResponseTrueByDefault() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithReplyChannel");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("ignoreEmptyResponses"));
		Assert.assertEquals(Boolean.FALSE, accessor.getPropertyValue("requiresReply"));
		context.close();
	}

	@Test
	public void simpleGatewayWithIgnoreEmptyResponses() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithIgnoreEmptyResponsesFalseAndRequiresReplyTrue");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(Boolean.FALSE, accessor.getPropertyValue("ignoreEmptyResponses"));
		Assert.assertEquals(Boolean.TRUE, accessor.getPropertyValue("requiresReply"));
		context.close();
	}

	@Test
	public void simpleGatewayWithDefaultSourceExtractor() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithDefaultSourceExtractor");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals("DefaultSourceExtractor", accessor.getPropertyValue("sourceExtractor").getClass().getSimpleName());
		context.close();
	}

	@Test
	public void simpleGatewayWithCustomSourceExtractor() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomSourceExtractor");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		SourceExtractor<?> sourceExtractor = (SourceExtractor<?>) context.getBean("sourceExtractor");
		assertEquals(sourceExtractor, accessor.getPropertyValue("sourceExtractor"));
		context.close();
	}

	@Test
	public void simpleGatewayWithCustomRequestCallback() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomRequestCallback");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		WebServiceMessageCallback callback = (WebServiceMessageCallback) context.getBean("requestCallback");
		assertEquals(callback, accessor.getPropertyValue("requestCallback"));
		context.close();
	}

	@Test
	public void simpleGatewayWithCustomMessageFactory() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomMessageFactory");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageFactory factory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(factory, accessor.getPropertyValue("messageFactory"));
		context.close();
	}

	@Test
	public void simpleGatewayWithCustomSourceExtractorAndMessageFactory() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
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
		context.close();
	}

	@Test
	public void simpleGatewayWithCustomFaultMessageResolver() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomFaultMessageResolver");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		FaultMessageResolver resolver = (FaultMessageResolver) context.getBean("faultMessageResolver");
		assertEquals(resolver, accessor.getPropertyValue("faultMessageResolver"));
		context.close();
	}


	@Test
	public void simpleGatewayWithCustomMessageSender() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomMessageSender");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageSender messageSender = (WebServiceMessageSender) context.getBean("messageSender");
		assertEquals(messageSender, ((WebServiceMessageSender[]) accessor.getPropertyValue("messageSenders"))[0]);
		context.close();
	}
	@Test
	public void simpleGatewayWithCustomMessageSenderList() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomMessageSenderList");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageSender messageSender = (WebServiceMessageSender) context.getBean("messageSender");
		assertEquals(messageSender, ((WebServiceMessageSender[]) accessor.getPropertyValue("messageSenders"))[0]);
		assertEquals("Wrong number of message senders ",
				2, ((WebServiceMessageSender[]) accessor.getPropertyValue("messageSenders")).length);
		context.close();
	}

	@Test
	public void simpleGatewayWithCustomInterceptor() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomInterceptor");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(SimpleWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		ClientInterceptor interceptor = context.getBean("interceptor", ClientInterceptor.class);
		assertEquals(interceptor, ((ClientInterceptor[]) accessor.getPropertyValue("interceptors"))[0]);
		context.close();
	}

	@Test
	public void simpleGatewayWithCustomInterceptorList() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
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
				2, ((ClientInterceptor[]) accessor.getPropertyValue("interceptors")).length);
		context.close();
	}

	@Test
	public void simpleGatewayWithPoller() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithPoller");
		assertEquals(PollingConsumer.class, endpoint.getClass());
		Object triggerObject = new DirectFieldAccessor(endpoint).getPropertyValue("trigger");
		assertEquals(PeriodicTrigger.class, triggerObject.getClass());
		PeriodicTrigger trigger = (PeriodicTrigger) triggerObject;
		DirectFieldAccessor accessor = new DirectFieldAccessor(trigger);
		assertEquals("PeriodicTrigger had wrong period",
				5000, ((Long) accessor.getPropertyValue("period")).longValue());
		context.close();
	}

	@Test
	public void simpleGatewayWithOrder() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithOrderAndAutoStartupFalse");
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(99, new DirectFieldAccessor(gateway).getPropertyValue("order"));
		context.close();
	}

	@Test
	public void simpleGatewayWithStartupFalse() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithOrderAndAutoStartupFalse");
		assertEquals(Boolean.FALSE, new DirectFieldAccessor(endpoint).getPropertyValue("autoStartup"));
		context.close();
	}

	@Test
	public void marshallingGatewayWithAllInOneMarshaller() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithAllInOneMarshaller");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MarshallingWebServiceOutboundGateway gateway = (MarshallingWebServiceOutboundGateway) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Marshaller marshaller = (Marshaller) context.getBean("marshallerAndUnmarshaller");
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "marshaller", Marshaller.class));
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "unmarshaller", Unmarshaller.class));
		context.close();
	}

	@Test
	public void marshallingGatewayWithSeparateMarshallerAndUnmarshaller() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithSeparateMarshallerAndUnmarshaller");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MarshallingWebServiceOutboundGateway gateway = (MarshallingWebServiceOutboundGateway) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Marshaller marshaller = (Marshaller) context.getBean("marshaller");
		Unmarshaller unmarshaller = (Unmarshaller) context.getBean("unmarshaller");
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "marshaller", Marshaller.class));
		assertEquals(unmarshaller, TestUtils.getPropertyValue(gateway, "unmarshaller", Unmarshaller.class));
		context.close();
	}

	@Test
	public void marshallingGatewayWithCustomRequestCallback() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithCustomRequestCallback");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		assertEquals(MarshallingWebServiceOutboundGateway.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		WebServiceMessageCallback callback = (WebServiceMessageCallback) context.getBean("requestCallback");
		assertEquals(callback, accessor.getPropertyValue("requestCallback"));
		context.close();
	}

	@Test
	public void marshallingGatewayWithAllInOneMarshallerAndMessageFactory() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithAllInOneMarshallerAndMessageFactory");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MarshallingWebServiceOutboundGateway gateway = (MarshallingWebServiceOutboundGateway) new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Marshaller marshaller = (Marshaller) context.getBean("marshallerAndUnmarshaller");
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "marshaller", Marshaller.class));
		assertEquals(marshaller, TestUtils.getPropertyValue(gateway, "unmarshaller", Unmarshaller.class));

		WebServiceMessageFactory messageFactory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(messageFactory, TestUtils.getPropertyValue(gateway, "webServiceTemplate.messageFactory"));
		context.close();
	}

	@Test
	public void marshallingGatewayWithSeparateMarshallerAndUnmarshallerAndMessageFactory() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
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
		context.close();
	}

	@Test
	public void simpleGatewayWithDestinationProvider() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
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
		assertEquals("Wrong DestinationProvider", stubProvider, destinationProviderObject);
		context.close();
	}

	@Test
	public void advised() {
		adviceCalled = 0;
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		AbstractEndpoint endpoint = (AbstractEndpoint) context.getBean("gatewayWithAdvice");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		MessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
		context.close();
	}

	@Test
	public void testInt2718AdvisedInsideAChain() {
		adviceCalled = 0;
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceOutboundGatewayParserTests.xml", this.getClass());
		MessageChannel channel = context.getBean("gatewayWithAdviceInsideAChain", MessageChannel.class);
		channel.send(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
		context.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void jmsUri() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
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
				Matchers.<WebServiceMessageExtractor<Object>>any());

		new DirectFieldAccessor(handler).setPropertyValue("webServiceTemplate", webServiceTemplate);

		handler.handleMessage(new GenericMessage<String>("foo"));

		verify(webServiceTemplate).sendAndReceive(eq("jms:wsQueue"),
				any(WebServiceMessageCallback.class),
				Matchers.<WebServiceMessageExtractor<Object>>any());
		context.close();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void invalidGatewayWithBothUriAndDestinationProvider() {
		new ClassPathXmlApplicationContext("invalidGatewayWithBothUriAndDestinationProvider.xml", this.getClass())
				.close();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void invalidGatewayWithNeitherUriNorDestinationProvider() {
		new ClassPathXmlApplicationContext("invalidGatewayWithNeitherUriNorDestinationProvider.xml", this.getClass())
				.close();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}

}
