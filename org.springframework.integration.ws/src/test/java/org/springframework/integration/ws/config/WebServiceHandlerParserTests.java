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

package org.springframework.integration.ws.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;
import org.springframework.integration.ws.handler.MarshallingWebServiceHandler;
import org.springframework.integration.ws.handler.SimpleWebServiceHandler;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * @author Mark Fisher
 */
public class WebServiceHandlerParserTests {

	@Test
	public void testSimpleWebServiceHandlerWithDefaultSourceExtractor() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithDefaultSourceExtractor");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(SimpleWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals("DefaultSourceExtractor", accessor.getPropertyValue("sourceExtractor").getClass().getSimpleName());
	}

	@Test
	public void testSimpleWebServiceHandlerWithCustomSourceExtractor() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithCustomSourceExtractor");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(SimpleWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		SourceExtractor sourceExtractor = (SourceExtractor) context.getBean("sourceExtractor");
		assertEquals(sourceExtractor, accessor.getPropertyValue("sourceExtractor"));
	}

	@Test
	public void testSimpleWebServiceHandlerWithCustomRequestCallback() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithCustomRequestCallback");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(SimpleWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		WebServiceMessageCallback callback = (WebServiceMessageCallback) context.getBean("requestCallback");
		assertEquals(callback, accessor.getPropertyValue("requestCallback"));
	}

	@Test
	public void testSimpleWebServiceHandlerWithCustomMessageFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithCustomMessageFactory");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(SimpleWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageFactory factory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(factory, accessor.getPropertyValue("messageFactory"));
	}

	@Test
	public void testSimpleWebServiceHandlerWithCustomSourceExtractorAndMessageFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithCustomSourceExtractorAndMessageFactory");
		SourceExtractor sourceExtractor = (SourceExtractor) context.getBean("sourceExtractor");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(SimpleWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		assertEquals(sourceExtractor, accessor.getPropertyValue("sourceExtractor"));
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageFactory factory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(factory, accessor.getPropertyValue("messageFactory"));
	}

	@Test
	public void testSimpleWebServiceHandlerWithCustomFaultMessageResolver() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithCustomFaultMessageResolver");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(SimpleWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		FaultMessageResolver resolver = (FaultMessageResolver) context.getBean("faultMessageResolver");
		assertEquals(resolver, accessor.getPropertyValue("faultMessageResolver"));
	}

	
	@Test
	public void testSimpleWebServiceHandlerWithCustomMessageSender() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithCustomMessageSender");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(SimpleWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);		
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageSender messageSender = (WebServiceMessageSender) context.getBean("messageSender");
		assertEquals(messageSender, ((WebServiceMessageSender[])accessor.getPropertyValue("messageSenders"))[0]);
	}
	@Test
	public void testSimpleWebServiceHandlerWithCustomMessageSenderList() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithCustomMessageSenderList");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(SimpleWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("webServiceTemplate"));
		WebServiceMessageSender messageSender = (WebServiceMessageSender) context.getBean("messageSender");
		assertEquals(messageSender, ((WebServiceMessageSender[])accessor.getPropertyValue("messageSenders"))[0]);
		assertEquals("Wrong number of message senders " ,2 , ((WebServiceMessageSender[])accessor.getPropertyValue("messageSenders")).length);
	}

	@Test
	public void testWebServiceHandlerWithAllInOneMarshaller() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithAllInOneMarshaller");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(MarshallingWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(gateway);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(
				gatewayAccessor.getPropertyValue("webServiceTemplate"));
		Marshaller marshaller = (Marshaller) context.getBean("marshallerAndUnmarshaller");
		assertEquals(marshaller, templateAccessor.getPropertyValue("marshaller"));
		assertEquals(marshaller, templateAccessor.getPropertyValue("unmarshaller"));
	}

	@Test
	public void testWebServiceHandlerWithSeparateMarshallerAndUnmarshaller() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithSeparateMarshallerAndUnmarshaller");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(MarshallingWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(gateway);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(
				gatewayAccessor.getPropertyValue("webServiceTemplate"));
		Marshaller marshaller = (Marshaller) context.getBean("marshaller");
		Unmarshaller unmarshaller = (Unmarshaller) context.getBean("unmarshaller");
		assertEquals(marshaller, templateAccessor.getPropertyValue("marshaller"));
		assertEquals(unmarshaller, templateAccessor.getPropertyValue("unmarshaller"));
	}

	@Test
	public void testMarshallingWebServiceHandlerWithCustomRequestCallback() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithCustomRequestCallback");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(MarshallingWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		WebServiceMessageCallback callback = (WebServiceMessageCallback) context.getBean("requestCallback");
		assertEquals(callback, accessor.getPropertyValue("requestCallback"));
	}

	@Test
	public void testWebServiceHandlerWithAllInOneMarshallerAndMessageFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithAllInOneMarshallerAndMessageFactory");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(MarshallingWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(gateway);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(
				gatewayAccessor.getPropertyValue("webServiceTemplate"));
		Marshaller marshaller = (Marshaller) context.getBean("marshallerAndUnmarshaller");
		assertEquals(marshaller, templateAccessor.getPropertyValue("marshaller"));
		assertEquals(marshaller, templateAccessor.getPropertyValue("unmarshaller"));
		WebServiceMessageFactory messageFactory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(messageFactory, templateAccessor.getPropertyValue("messageFactory"));
	}

	@Test
	public void testWebServiceHandlerWithSeparateMarshallerAndUnmarshallerAndMessageFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceHandlerParserTests.xml", this.getClass());
		MessageEndpoint endpoint = (MessageEndpoint) context.getBean("handlerWithSeparateMarshallerAndUnmarshallerAndMessageFactory");
		assertEquals(SubscribingConsumerEndpoint.class, endpoint.getClass());
		Object gateway = new DirectFieldAccessor(endpoint).getPropertyValue("consumer");
		assertEquals(MarshallingWebServiceHandler.class, gateway.getClass());
		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(gateway);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(
				gatewayAccessor.getPropertyValue("webServiceTemplate"));
		Marshaller marshaller = (Marshaller) context.getBean("marshaller");
		Unmarshaller unmarshaller = (Unmarshaller) context.getBean("unmarshaller");
		assertEquals(marshaller, templateAccessor.getPropertyValue("marshaller"));
		assertEquals(unmarshaller, templateAccessor.getPropertyValue("unmarshaller"));
		WebServiceMessageFactory messageFactory = (WebServiceMessageFactory) context.getBean("messageFactory");
		assertEquals(messageFactory, templateAccessor.getPropertyValue("messageFactory"));
	}

}
