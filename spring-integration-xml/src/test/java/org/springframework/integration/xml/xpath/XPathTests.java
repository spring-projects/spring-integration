/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.xpath;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.xml.xpath.NodeMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class XPathTests {

	private static final String XML = """
			<parent>
				<child name='test' age='42' married='true'/>
			</parent>
			""";

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
		Object result = XPathUtils.evaluate(XML, "/parent/child/@name");
		assertThat(result).isEqualTo("test");

		result = XPathUtils.evaluate(XML, "/parent/child/@name", "string");
		assertThat(result).isEqualTo("test");

		result = XPathUtils.evaluate(XML, "/parent/child/@age", "number");
		assertThat(result).isEqualTo((double) 42);

		result = XPathUtils.evaluate(XML, "/parent/child/@married = 'true'", "boolean");
		assertThat(result).isEqualTo(Boolean.TRUE);

		result = XPathUtils.evaluate(XML, "/parent/child", "node");
		assertThat(result).isInstanceOf(Node.class);
		Node node = (Node) result;
		assertThat(node.getLocalName()).isEqualTo("child");
		assertThat(node.getAttributes().getNamedItem("name").getTextContent()).isEqualTo("test");
		assertThat(node.getAttributes().getNamedItem("age").getTextContent()).isEqualTo("42");
		assertThat(node.getAttributes().getNamedItem("married").getTextContent()).isEqualTo("true");

		result = XPathUtils.evaluate("<parent><child name='foo'/><child name='bar'/></parent>", "/parent/child",
				"node_list");
		assertThat(result).isInstanceOf(List.class);
		List<Node> nodeList = (List<Node>) result;
		assertThat(nodeList.size()).isEqualTo(2);
		Node node1 = nodeList.get(0);
		Node node2 = nodeList.get(1);
		assertThat(node1.getLocalName()).isEqualTo("child");
		assertThat(node1.getAttributes().getNamedItem("name").getTextContent()).isEqualTo("foo");
		assertThat(node2.getLocalName()).isEqualTo("child");
		assertThat(node2.getAttributes().getNamedItem("name").getTextContent()).isEqualTo("bar");

		result = XPathUtils.evaluate("""
				<parent>
					<child name='foo'/>
					<child name='bar'/>
				</parent>
				""", "/parent/child", "document_list");
		assertThat(result).isInstanceOf(List.class);
		List<Document> documentList = (List<Document>) result;
		assertThat(documentList.size()).isEqualTo(2);
		Node document1 = documentList.get(0);
		Node document2 = documentList.get(1);
		assertThat(document1.getFirstChild().getLocalName()).isEqualTo("child");
		assertThat(document1.getFirstChild().getAttributes().getNamedItem("name").getTextContent()).isEqualTo("foo");
		assertThat(document2.getFirstChild().getLocalName()).isEqualTo("child");
		assertThat(document2.getFirstChild().getAttributes().getNamedItem("name").getTextContent()).isEqualTo("bar");

		result = XPathUtils.evaluate(XML, "/parent/child/@name", new TestNodeMapper());
		assertThat(result).isEqualTo("test-mapped");

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> XPathUtils.evaluate(new Date(), "/parent/child"))
				.withMessageContaining("unsupported payload type");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> XPathUtils.evaluate(XML, "/parent/child", "string", "number"))
				.withMessage("'resultArg' can contains only one element.");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> XPathUtils.evaluate(XML, "/parent/child", "foo"))
				.withMessage("'resultArg[0]' can be an instance of 'NodeMapper<?>' or " +
						"one of supported String constants: [string, boolean, number, node, node_list, document_list]");
	}

	@Test
	public void testInt3140Transformer() {
		Message<?> message = MessageBuilder.withPayload("<person name='John Doe' age='42' married='true'/>")
				.setHeader("xpath", "/person/@age")
				.build();

		this.xpathTransformerInput.send(message);

		Message<?> receive = this.channelA.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("42-mapped");
	}

	@Test
	public void testInt3140Filter() {
		this.xpathFilterInput.send(new GenericMessage<Object>("<name>outputOne</name>"));
		this.xpathFilterInput.send(new GenericMessage<Object>("<other>outputOne</other>"));

		Message<?> receive = this.channelA.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("<name>outputOne</name>");

		receive = this.channelZ.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("<other>outputOne</other>");
	}

	@Test
	public void testInt3140Splitter() {
		StringSourceFactory stringSourceFactory = new StringSourceFactory();
		this.xpathSplitterInput.send(new GenericMessage<Object>("<books><book>book1</book><book>book2</book></books>"));

		Message<?> receive = this.channelA.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(stringSourceFactory.createSource(receive.getPayload()).toString()).contains("<book>book1</book>");

		receive = this.channelA.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(stringSourceFactory.createSource(receive.getPayload()).toString()).contains("<book>book2</book>");
	}

	@Test
	public void testInt3140Router() {
		this.xpathRouterInput.send(new GenericMessage<Object>("<name>A</name>"));
		this.xpathRouterInput.send(new GenericMessage<Object>("<name>B</name>"));
		this.xpathRouterInput.send(new GenericMessage<Object>("<name>X</name>"));

		Message<?> receive = this.channelA.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("<name>A</name>");

		receive = this.channelB.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("<name>B</name>");

		receive = this.channelZ.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("<name>X</name>");
	}

	public static class TestNodeMapper implements NodeMapper<String> {

		@Override
		public String mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent() + "-mapped";
		}

	}

}
