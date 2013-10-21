/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.xml.util.XmlTestUtil;

/**
 * @author Jonas Partner
 */
public class XPathMessageSplitterTests {
	
	private String splittingXPath = "/orders/order";
	
	private XPathMessageSplitter splitter;

	private QueueChannel replyChannel = new QueueChannel();


	@Before
	public void setUp(){
		splitter = new XPathMessageSplitter(splittingXPath);
		splitter.setOutputChannel(replyChannel);
	}


	@Test
	public void splitDocument() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<orders><order>one</order><order>two</order><order>three</order></orders>");
		splitter.handleMessage(new GenericMessage<Document>(doc));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			assertTrue("unexpected payload type" + message.getPayload().getClass().getName(), message.getPayload() instanceof Node);
			assertFalse("unexpected payload type" + message.getPayload().getClass().getName(), message.getPayload() instanceof Document);
		}
	}

	@Test(expected = MessagingException.class)
	public void splitDocumentThatDoesNotMatch() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<wrongDocument/>");
		splitter.handleMessage(new GenericMessage<Document>(doc));
	}

	@Test
	public void splitDocumentWithCreateDocumentsTrue() throws Exception {
		splitter.setCreateDocuments(true);
		Document doc = XmlTestUtil.getDocumentForString("<orders><order>one</order><order>two</order><order>three</order></orders>");
		splitter.handleMessage(new GenericMessage<Document>(doc));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			assertTrue("unexpected payload type" + message.getPayload().getClass().getName(), message.getPayload() instanceof Document);
			Document docPayload = (Document)message.getPayload();
			assertEquals("Wrong root element name" ,"order", docPayload.getDocumentElement().getLocalName());
		}
	}

	@Test
	public void splitStringXml() throws Exception {
		String payload = "<orders><order>one</order><order>two</order><order>three</order></orders>";
		splitter.handleMessage(new GenericMessage<String>(payload));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			assertTrue("unexpected payload type " + message.getPayload().getClass().getName(), message.getPayload() instanceof String);
		}
	}

	@Test(expected = MessagingException.class)
	public void invalidPayloadType() {
		splitter.handleMessage(new GenericMessage<Integer>(123));
	}

}
