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
package org.springframework.integration.xml.splitter;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XPathMessageSplitterTests {
	
	String splittingXPath = "/orders/order";
	
	XPathMessageSplitter splitter;
	
	@Before
	public void setUp(){
		splitter = new XPathMessageSplitter(splittingXPath);
	}
	
	@Test
	public void splitDocument() throws Exception{
		Document doc = XmlTestUtil.getDocumentForString("<orders><order>one</order><order>two</order><order>three</order></orders>");
		List<Message<?>> docMessages = splitter.split(new GenericMessage<Document>(doc));
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			assertTrue("unexpected payload type" + message.getPayload().getClass().getName(), message.getPayload() instanceof Node);
			assertFalse("unexpected payload type" + message.getPayload().getClass().getName(), message.getPayload() instanceof Document);
		}
	}
	
	@Test(expected=MessagingException.class)
	public void splitDocumentThatDoesNotMatch() throws Exception{
		Document doc = XmlTestUtil.getDocumentForString("<wrongDocument/>");
		splitter.split(new GenericMessage<Document>(doc));
	}
	
	@Test
	public void splitDocumentWithCreateDocumentsTrue() throws Exception{
		splitter.setCreateDocuments(true);
		Document doc = XmlTestUtil.getDocumentForString("<orders><order>one</order><order>two</order><order>three</order></orders>");
		List<Message<?>> docMessages = splitter.split(new GenericMessage<Document>(doc));
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			assertTrue("unexpected payload type" + message.getPayload().getClass().getName(), message.getPayload() instanceof Document);
		}
	}
	
	
	@Test
	public void splitStringXml() throws Exception{
		List<Message<?>> docMessages = splitter.split(new GenericMessage<String>("<orders><order>one</order><order>two</order><order>three</order></orders>"));
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			System.out.println(message);
			assertTrue("unexpected payload type " + message.getPayload().getClass().getName(), message.getPayload() instanceof String);
		}
	}

}
