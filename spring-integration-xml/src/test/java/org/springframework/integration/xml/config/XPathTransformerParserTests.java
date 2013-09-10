/*
 * Copyright 2002-2010 the original author or authors.
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

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xml.xpath.NodeMapper;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class XPathTransformerParserTests {

	@Autowired
	private MessageChannel defaultInput;

	@Autowired
	private MessageChannel numberInput;

	@Autowired
	private MessageChannel booleanInput;

	@Autowired
	private MessageChannel nodeInput;

	@Autowired
	private MessageChannel nodeListInput;

	@Autowired
	private MessageChannel nodeMapperInput;

	@Autowired
	private MessageChannel customConverterInput;

	@Autowired
	private MessageChannel expressionRefInput;

	@Autowired
	private PollableChannel output;

	private final Message<?> message = MessageBuilder.withPayload("<person name='John Doe' age='42' married='true'/>").build();


	@Test
	public void stringResultByDefault() {
		this.defaultInput.send(message);
		assertEquals("John Doe", output.receive(0).getPayload());
	}

	@Test
	public void numberResult() {
		this.numberInput.send(message);
		assertEquals(new Double(42), output.receive(0).getPayload());
	}

	@Test
	public void booleanResult() {
		this.booleanInput.send(message);
		assertEquals(Boolean.TRUE, output.receive(0).getPayload());
	}

	@Test
	public void nodeResult() {
		this.nodeInput.send(message);
		Object payload = output.receive(0).getPayload();
		assertTrue(payload instanceof Node);
		Node node = (Node) payload;
		assertEquals("42", node.getTextContent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nodeListResult() {
		this.nodeListInput.send(message);
		Object payload = output.receive(0).getPayload();
		assertTrue(List.class.isAssignableFrom(payload.getClass()));
		List<Node> nodeList = (List<Node>) payload;
		assertEquals(3, nodeList.size());
	}

	@Test
	public void nodeMapper() {
		this.nodeMapperInput.send(message);
		assertEquals("42-mapped", output.receive(0).getPayload());
	}

	@Test
	public void customConverter() {
		this.customConverterInput.send(message);
		assertEquals("custom", output.receive(0).getPayload());
	}

	@Test
	public void expressionRef() {
		this.expressionRefInput.send(message);
		assertEquals(new Double(84), output.receive(0).getPayload());
	}


	@SuppressWarnings("unused")
	private static class TestNodeMapper implements NodeMapper<Object> {

		public Object mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent() + "-mapped";
		}
	}


	@SuppressWarnings("unused")
	private static class TestXmlPayloadConverter implements XmlPayloadConverter {

		public Source convertToSource(Object object) {
			throw new UnsupportedOperationException();
		}

		public Node convertToNode(Object object) {
			try {
				return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
						new InputSource(new StringReader("<test type='custom'/>")));
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		public Document convertToDocument(Object object) {
			throw new UnsupportedOperationException();
		}
	}

}
