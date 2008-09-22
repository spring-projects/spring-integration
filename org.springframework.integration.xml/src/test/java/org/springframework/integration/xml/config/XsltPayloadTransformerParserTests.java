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

package org.springframework.integration.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.transform.dom.DOMResult;

import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Document;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.transformer.Transformer;
import org.springframework.integration.xml.config.StubResultFactory.StubStringResult;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 */
public class XsltPayloadTransformerParserTests {

	private String doc = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";

	private ApplicationContext applicationContext;


	@Before
	public void setUp() {
		applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
	}


	@Test
	public void testWithResourceProvided() throws Exception {
		Transformer transformer = (Transformer) applicationContext
				.getBean("xsltTransformerWithResource");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		Message<?> result = transformer.transform(message);
		assertTrue("Payload was not a DOMResult", result.getPayload() instanceof DOMResult);
		Document doc = (Document) ((DOMResult) result.getPayload()).getNode();
		assertEquals("Wrong payload", "test", doc.getDocumentElement().getTextContent());
	}

	@Test
	public void testWithTemplatesProvided() throws Exception {
		Transformer transformer = (Transformer) applicationContext
				.getBean("xsltTransformerWithTemplates");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		Message<?> result = transformer.transform(message);
		assertTrue("Payload was not a DOMResult", result.getPayload() instanceof DOMResult);
		Document doc = (Document) ((DOMResult) result.getPayload()).getNode();
		assertEquals("Wrong payload", "test", doc.getDocumentElement().getTextContent());
	}

	@Test
	public void testWithTemplatesAndResultTransformer() throws Exception {
		Transformer transformer = (Transformer) applicationContext
				.getBean("xsltTransformerWithTemplatesAndResultTransformer");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		Message<?> result = transformer.transform(message);
		assertEquals("Wrong payload type", String.class, result.getPayload().getClass());
		String strResult = (String)result.getPayload();
		assertEquals("Wrong payload", "testReturn", strResult);
	}
	
	@Test
	public void testWithResourceProvidedndStubResultFactory() throws Exception {
		Transformer transformer = (Transformer) applicationContext
				.getBean("xsltTransformerWithTemplatesAndResultFactory");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		Message<?> result = transformer.transform(message);
		assertTrue("Payload was not a StubStringResult", result.getPayload() instanceof StubStringResult);
	}
	
	@Test
	public void testWithResourceAndStringResultType() throws Exception {
		Transformer transformer = (Transformer) applicationContext
				.getBean("xsltTransformerWithTemplatesAndStringResulType");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		Message<?> result = transformer.transform(message);
		assertTrue("Payload was not a StringResult", result.getPayload() instanceof StringResult);
	}
	
	
		


	
}
