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
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.ws.handler.MarshallingWebServiceHandler;
import org.springframework.integration.ws.handler.SimpleWebServiceHandler;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.client.core.SourceExtractor;

/**
 * @author Mark Fisher
 */
public class WebServiceHandlerParserTests {

	@Test
	public void testSimpleWebServiceHandlerWithDefaultSourceExtractor() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageHandler handler = (MessageHandler) context.getBean("handlerWithDefaultSourceExtractor");
		assertEquals(SimpleWebServiceHandler.class, handler.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		assertEquals("DefaultSourceExtractor", accessor.getPropertyValue("sourceExtractor").getClass().getSimpleName());
	}

	@Test
	public void testSimpleWebServiceHandlerWithCustomSourceExtractor() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"simpleWebServiceHandlerParserTests.xml", this.getClass());
		MessageHandler handler = (MessageHandler) context.getBean("handlerWithCustomSourceExtractor");
		assertEquals(SimpleWebServiceHandler.class, handler.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		SourceExtractor sourceExtractor = (SourceExtractor) context.getBean("sourceExtractor");
		assertEquals(sourceExtractor, accessor.getPropertyValue("sourceExtractor"));
	}

	@Test
	public void testWebServiceHandlerWithAllInOneMarshaller() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceHandlerParserTests.xml", this.getClass());
		MessageHandler handler = (MessageHandler) context.getBean("handlerWithAllInOneMarshaller");
		assertEquals(MarshallingWebServiceHandler.class, handler.getClass());
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(
				handlerAccessor.getPropertyValue("webServiceTemplate"));
		Marshaller marshaller = (Marshaller) context.getBean("marshallerAndUnmarshaller");
		assertEquals(marshaller, templateAccessor.getPropertyValue("marshaller"));
		assertEquals(marshaller, templateAccessor.getPropertyValue("unmarshaller"));
	}

	@Test
	public void testWebServiceTargetAdapterWithSeparateMarshallerAndUnmarshaller() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"marshallingWebServiceHandlerParserTests.xml", this.getClass());
		MessageHandler handler = (MessageHandler) context.getBean("handlerWithSeparateMarshallerAndUnmarshaller");
		assertEquals(MarshallingWebServiceHandler.class, handler.getClass());
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		DirectFieldAccessor templateAccessor = new DirectFieldAccessor(
				handlerAccessor.getPropertyValue("webServiceTemplate"));
		Marshaller marshaller = (Marshaller) context.getBean("marshaller");
		Unmarshaller unmarshaller = (Unmarshaller) context.getBean("unmarshaller");
		assertEquals(marshaller, templateAccessor.getPropertyValue("marshaller"));
		assertEquals(unmarshaller, templateAccessor.getPropertyValue("unmarshaller"));
	}

}
