/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.xml.xpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.integration.xml.xpath.XPathUtils.evaluate;

import java.util.Date;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xml.source.StringSourceFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xml.xpath.NodeMapper;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class XPathTests {

	private static final String XML = "<parent><child name='test' age='42' married='true'/></parent>";

	@Autowired
	private PollableChannel channelA;

	@Autowired
	private PollableChannel channelB;

	@Autowired
	private PollableChannel channelZ;

	@Autowired
	private MessageChannel xpathTransformerInput;

	@Autowired
	private MessageChannel xpathFilterInput;

	@Autowired
	private MessageChannel xpathSplitterInput;

	@Autowired
	private MessageChannel xpathRouterInput;

	@Test
	@SuppressWarnings("unchecked")
	public void testXPathUtils() {
		Object result = evaluate(XML, "/parent/child/@name");
		assertEquals("test", result);

		result = evaluate(XML, "/parent/child/@name", "string");
		assertEquals("test", result);

		result = evaluate(XML, "/parent/child/@age", "number");
		assertEquals((double) 42, result);

		result = evaluate(XML, "/parent/child/@married = 'true'", "boolean");
		assertEquals(Boolean.TRUE, result);

		result = evaluate(XML, "/parent/child", "node");
		assertThat(result, Matchers.instanceOf(Node.class));
		Node node = (Node) result;
		assertEquals("child", node.getLocalName());
		assertEquals("test", node.getAttributes().getNamedItem("name").getTextContent());
		assertEquals("42", node.getAttributes().getNamedItem("age").getTextContent());
		assertEquals("true", node.getAttributes().getNamedItem("married").getTextContent());

		result = evaluate("<parent><child name='foo'/><child name='bar'/></parent>", "/parent/child", "node_list");
		assertThat(result, Matchers.instanceOf(List.class));
		List<Node> nodeList = (List<Node>) result;
		assertEquals(2, nodeList.size());
		Node node1 = nodeList.get(0);
		Node node2 = nodeList.get(1);
		assertEquals("child", node1.getLocalName());
		assertEquals("foo", node1.getAttributes().getNamedItem("name").getTextContent());
		assertEquals("child", node2.getLocalName());
		assertEquals("bar", node2.getAttributes().getNamedItem("name").getTextContent());

		result = evaluate("<parent><child name='foo'/><child name='bar'/></parent>", "/parent/child", "document_list");
		assertThat(result, Matchers.instanceOf(List.class));
		List<Document> documentList = (List<Document>) result;
		assertEquals(2, documentList.size());
		Node document1 = documentList.get(0);
		Node document2 = documentList.get(1);
		assertEquals("child", document1.getFirstChild().getLocalName());
		assertEquals("foo", document1.getFirstChild().getAttributes().getNamedItem("name").getTextContent());
		assertEquals("child", document2.getFirstChild().getLocalName());
		assertEquals("bar", document2.getFirstChild().getAttributes().getNamedItem("name").getTextContent());

		result = evaluate(XML, "/parent/child/@name", new TestNodeMapper());
		assertEquals("test-mapped", result);

		try {
			evaluate(new Date(), "/parent/child");
			fail("MessagingException expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessagingException.class));
			assertThat(e.getMessage(), Matchers.containsString("unsupported payload type"));
		}

		try {
			evaluate(XML, "/parent/child", "string", "number");
			fail("MessagingException expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(IllegalArgumentException.class));
			assertEquals("'resultArg' can contains only one element.", e.getMessage());
		}

		try {
			evaluate(XML, "/parent/child", "foo");
			fail("MessagingException expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(IllegalArgumentException.class));
			assertEquals("'resultArg[0]' can be an instance of 'NodeMapper<?>' or " +
					"one of supported String constants: [string, boolean, number, node, node_list, document_list]", e.getMessage());
		}

	}

	@Test
	public void testInt3140Transformer() {
		Message<?> message = MessageBuilder.withPayload("<person name='John Doe' age='42' married='true'/>")
				.setHeader("xpath", "/person/@age")
				.build();

		this.xpathTransformerInput.send(message);

		Message<?> receive = this.channelA.receive(1000);
		assertNotNull(receive);
		assertEquals("42-mapped", receive.getPayload());
	}

	@Test
	public void testInt3140Filter() {
		this.xpathFilterInput.send(new GenericMessage<Object>("<name>outputOne</name>"));
		this.xpathFilterInput.send(new GenericMessage<Object>("<other>outputOne</other>"));

		Message<?> receive = this.channelA.receive(1000);
		assertNotNull(receive);
		assertEquals("<name>outputOne</name>", receive.getPayload());

		receive = this.channelZ.receive(1000);
		assertNotNull(receive);
		assertEquals("<other>outputOne</other>", receive.getPayload());
	}

	@Test
	public void testInt3140Splitter() {
		StringSourceFactory stringSourceFactory = new StringSourceFactory();
		this.xpathSplitterInput.send(new GenericMessage<Object>("<books><book>book1</book><book>book2</book></books>"));

		Message<?> receive = this.channelA.receive(1000);
		assertNotNull(receive);
		assertThat(stringSourceFactory.createSource(receive.getPayload()).toString(), Matchers.containsString("<book>book1</book>"));

		receive = this.channelA.receive(1000);
		assertNotNull(receive);
		assertThat(stringSourceFactory.createSource(receive.getPayload()).toString(), Matchers.containsString("<book>book2</book>"));
	}


	@Test
	public void testInt3140Router() {
		this.xpathRouterInput.send(new GenericMessage<Object>("<name>A</name>"));
		this.xpathRouterInput.send(new GenericMessage<Object>("<name>B</name>"));
		this.xpathRouterInput.send(new GenericMessage<Object>("<name>X</name>"));

		Message<?> receive = this.channelA.receive(1000);
		assertNotNull(receive);
		assertEquals("<name>A</name>", receive.getPayload());

		receive = this.channelB.receive(1000);
		assertNotNull(receive);
		assertEquals("<name>B</name>", receive.getPayload());

		receive = this.channelZ.receive(1000);
		assertNotNull(receive);
		assertEquals("<name>X</name>", receive.getPayload());
	}


	public static class TestNodeMapper implements NodeMapper<String> {

		@Override
		public String mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent() + "-mapped";
		}

	}

}
