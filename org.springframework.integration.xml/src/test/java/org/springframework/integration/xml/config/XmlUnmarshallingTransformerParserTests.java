/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.xml.config;

import static org.junit.Assert.*;

import javax.xml.transform.dom.DOMSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.transformer.MessageTransformer;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringSource;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class XmlUnmarshallingTransformerParserTests {

	ApplicationContext appContext;

	StubUnmarshaller unmarshaller;

	@Before
	public void setUp() {
		appContext = new ClassPathXmlApplicationContext("XmlUnmarshallingTransformerParserTests-context.xml",
				getClass());
		unmarshaller = (StubUnmarshaller) appContext.getBean("unmarshaller");
	}

	@Test
	public void testDefaultUnmarshall() throws Exception {
		MessageTransformer transformer = (MessageTransformer) appContext.getBean("defaultUnmarshaller");
		GenericMessage<Object> message = new GenericMessage<Object>(new StringSource(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>"));
		transformer.transform(message);
		assertEquals("Wrong payload after unmarshalling ", "unmarshalled", message.getPayload());
		assertTrue("Wrong source passed to unmarshaller", unmarshaller.sourcesPassed.poll() instanceof StringSource);
	}

	@Test
	public void testUnmarshallString() throws Exception {
		MessageTransformer transformer = (MessageTransformer) appContext.getBean("defaultUnmarshaller");
		GenericMessage<Object> message = new GenericMessage<Object>(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>");
		transformer.transform(message);
		assertEquals("Wrong payload after unmarshalling ", "unmarshalled", message.getPayload());
		assertTrue("Wrong source passed to unmarshaller", unmarshaller.sourcesPassed.poll() instanceof DOMSource);
	}

	@Test
	public void testUnmarshallDocument() throws Exception {
		MessageTransformer transformer = (MessageTransformer) appContext.getBean("defaultUnmarshaller");
		GenericMessage<Object> message = new GenericMessage<Object>(
				XmlTestUtil
						.getDocumentForString("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>"));
		transformer.transform(message);
		assertEquals("Wrong payload after unmarshalling ", "unmarshalled", message.getPayload());
		assertTrue("Wrong source passed to unmarshaller", unmarshaller.sourcesPassed.poll() instanceof DOMSource);
	}

	@Test(expected = MessagingException.class)
	public void testUnmarshallUnsupported() throws Exception {
		MessageTransformer transformer = (MessageTransformer) appContext.getBean("defaultUnmarshaller");
		GenericMessage<Object> message = new GenericMessage<Object>(new StringBuffer(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>"));
		transformer.transform(message);
	}

}
