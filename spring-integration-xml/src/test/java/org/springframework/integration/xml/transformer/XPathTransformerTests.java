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

package org.springframework.integration.xml.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.springframework.messaging.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.integration.xml.xpath.XPathEvaluationType;
import org.springframework.xml.xpath.NodeMapper;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class XPathTransformerTests {

	private static final String XML = "<parent><child name='test' age='42' married='true'/></parent>";

	private volatile Message<?> message;


	@Before
	public void createMessage() {
		this.message = MessageBuilder.withPayload(XML).build();
	}


	@Test
	public void stringResultTypeByDefault() throws Exception {
		XPathTransformer transformer = new XPathTransformer("/parent/child/@name");
		Object result = transformer.doTransform(message);
		assertEquals("test", result);
	}

	@Test
	public void xpathExpressionReferenceConstructorInsteadOfString() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/parent/child/@name");
		XPathTransformer transformer = new XPathTransformer(expression);
		Object result = transformer.doTransform(message);
		assertEquals("test", result);
	}

	@Test
	public void numberResult() throws Exception {
		XPathTransformer transformer = new XPathTransformer("/parent/child/@age");
		transformer.setEvaluationType(XPathEvaluationType.NUMBER_RESULT);
		Object result = transformer.doTransform(message);
		assertEquals(new Double(42), result);
	}

	@Test
	public void booleanResult() throws Exception {
		XPathTransformer transformer = new XPathTransformer("/parent/child/@married = 'true'");
		transformer.setEvaluationType(XPathEvaluationType.BOOLEAN_RESULT);
		Object result = transformer.doTransform(message);
		assertEquals(Boolean.TRUE, result);
	}

	@Test
	public void nodeResult() throws Exception {
		XPathTransformer transformer = new XPathTransformer("/parent/child");
		transformer.setEvaluationType(XPathEvaluationType.NODE_RESULT);
		Object result = transformer.doTransform(message);
		assertTrue(result instanceof Node);
		Node node = (Node) result;
		assertEquals("child", node.getLocalName());
		assertEquals("test", node.getAttributes().getNamedItem("name").getTextContent());
		assertEquals("42", node.getAttributes().getNamedItem("age").getTextContent());
		assertEquals("true", node.getAttributes().getNamedItem("married").getTextContent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nodeListResult() throws Exception {
		XPathTransformer transformer = new XPathTransformer("/parent/child");
		transformer.setEvaluationType(XPathEvaluationType.NODE_LIST_RESULT);
		Message<?> message = MessageBuilder.withPayload(
				"<parent><child name='foo'/><child name='bar'/></parent>").build();
		Object result = transformer.doTransform(message);
		assertTrue(List.class.isAssignableFrom(result.getClass()));
		List<Node> nodeList = (List<Node>) result;
		assertEquals(2, nodeList.size());
		Node node1 = nodeList.get(0);
		Node node2 = nodeList.get(1);
		assertEquals("child", node1.getLocalName());
		assertEquals("foo", node1.getAttributes().getNamedItem("name").getTextContent());
		assertEquals("child", node2.getLocalName());
		assertEquals("bar", node2.getAttributes().getNamedItem("name").getTextContent());
	}

	@Test
	public void nodeMapper() throws Exception {
		XPathTransformer transformer = new XPathTransformer("/parent/child/@name");
		transformer.setNodeMapper(new TestNodeMapper());
		Object result = transformer.doTransform(message);
		assertEquals("test-mapped", result);
	}

	@Test
	public void customConverter() throws Exception {
		XPathTransformer transformer = new XPathTransformer("/test/@type");
		transformer.setConverter(new TestXmlPayloadConverter());
		Object result = transformer.doTransform(message);
		assertEquals("custom", result);
	}


	private static class TestNodeMapper implements NodeMapper<Object> {

		public Object mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent() + "-mapped";
		}
	}


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
