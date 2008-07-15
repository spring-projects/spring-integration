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

package org.springframework.integration.xml.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;

import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Document;

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 */
public class ResultToDocumentTransformerTests {

	String doc = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";

	ResultToDocumentTransformer resToDocTransformer;


	@Before
	public void setUp() {
		resToDocTransformer = new ResultToDocumentTransformer();
	}


	@Test
	public void testWithDomResult() throws Exception {
		DOMResult result = XmlTestUtil.getDomResultForString(doc);
		GenericMessage<Object> message = new GenericMessage<Object>(result);
		Message<?> transformed = resToDocTransformer.transform(message);
		assertTrue("Wrong payload type expected Document", transformed.getPayload() instanceof Document);
		Document doc = (Document) transformed.getPayload();
		assertEquals("Wrong root element name", "order", doc.getDocumentElement().getNodeName());
	}

	@Test
	public void testWithStringResult() throws Exception {
		StringResult result = XmlTestUtil.getStringResultForString(doc);
		GenericMessage<Object> message = new GenericMessage<Object>(result);
		Message<?> transformed = resToDocTransformer.transform(message);
		assertTrue("Wrong payload type expected Document", transformed.getPayload() instanceof Document);
		Document doc = (Document) transformed.getPayload();
		assertEquals("Wrong root element name", "order", doc.getDocumentElement().getNodeName());
	}

	@Test(expected = MessagingException.class)
	public void testWithUnsupportedSaxResult() throws Exception {
		SAXResult result = new SAXResult();
		GenericMessage<Object> message = new GenericMessage<Object>(result);
		resToDocTransformer.transform(message);
	}
}
