/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.integration.xml.xpath.XPathEvaluationType;
import org.springframework.messaging.Message;
import org.springframework.xml.xpath.NodeMapper;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class XPathTransformerTests {

	private static final String XML = "<parent><child name='test' age='42' married='true'/></parent>";

	private volatile Message<?> message;


	@BeforeEach
	public void createMessage() {
		this.message = MessageBuilder.withPayload(XML).build();
	}


	@Test
	public void stringResultTypeByDefault() {
		XPathTransformer transformer = new XPathTransformer("/parent/child/@name");
		Object result = transformer.doTransform(message);
		assertThat(result).isEqualTo("test");
	}

	@Test
	public void xpathExpressionReferenceConstructorInsteadOfString() {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/parent/child/@name");
		XPathTransformer transformer = new XPathTransformer(expression);
		Object result = transformer.doTransform(message);
		assertThat(result).isEqualTo("test");
	}

	@Test
	public void numberResult() {
		XPathTransformer transformer = new XPathTransformer("/parent/child/@age");
		transformer.setEvaluationType(XPathEvaluationType.NUMBER_RESULT);
		Object result = transformer.doTransform(message);
		assertThat(result).isEqualTo(42d);
	}

	@Test
	public void booleanResult() {
		XPathTransformer transformer = new XPathTransformer("/parent/child/@married = 'true'");
		transformer.setEvaluationType(XPathEvaluationType.BOOLEAN_RESULT);
		Object result = transformer.doTransform(message);
		assertThat(result).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void nodeResult() {
		XPathTransformer transformer = new XPathTransformer("/parent/child");
		transformer.setEvaluationType(XPathEvaluationType.NODE_RESULT);
		Object result = transformer.doTransform(message);
		assertThat(result instanceof Node).isTrue();
		Node node = (Node) result;
		assertThat(node.getLocalName()).isEqualTo("child");
		assertThat(node.getAttributes().getNamedItem("name").getTextContent()).isEqualTo("test");
		assertThat(node.getAttributes().getNamedItem("age").getTextContent()).isEqualTo("42");
		assertThat(node.getAttributes().getNamedItem("married").getTextContent()).isEqualTo("true");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nodeListResult() {
		XPathTransformer transformer = new XPathTransformer("/parent/child");
		transformer.setEvaluationType(XPathEvaluationType.NODE_LIST_RESULT);
		Message<?> message = MessageBuilder.withPayload(
				"<parent><child name='foo'/><child name='bar'/></parent>").build();
		Object result = transformer.doTransform(message);
		assertThat(List.class.isAssignableFrom(result.getClass())).isTrue();
		List<Node> nodeList = (List<Node>) result;
		assertThat(nodeList.size()).isEqualTo(2);
		Node node1 = nodeList.get(0);
		Node node2 = nodeList.get(1);
		assertThat(node1.getLocalName()).isEqualTo("child");
		assertThat(node1.getAttributes().getNamedItem("name").getTextContent()).isEqualTo("foo");
		assertThat(node2.getLocalName()).isEqualTo("child");
		assertThat(node2.getAttributes().getNamedItem("name").getTextContent()).isEqualTo("bar");
	}

	@Test
	public void nodeMapper() {
		XPathTransformer transformer = new XPathTransformer("/parent/child/@name");
		transformer.setNodeMapper(new TestNodeMapper());
		Object result = transformer.doTransform(message);
		assertThat(result).isEqualTo("test-mapped");
	}

	@Test
	public void customConverter() {
		XPathTransformer transformer = new XPathTransformer("/test/@type");
		transformer.setConverter(new TestXmlPayloadConverter());
		Object result = transformer.doTransform(message);
		assertThat(result).isEqualTo("custom");
	}


	private static class TestNodeMapper implements NodeMapper<Object> {

		TestNodeMapper() {
			super();
		}

		@Override
		public Object mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent() + "-mapped";
		}

	}


	private static class TestXmlPayloadConverter implements XmlPayloadConverter {

		TestXmlPayloadConverter() {
			super();
		}

		@Override
		public Source convertToSource(Object object) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Node convertToNode(Object object) {
			try {
				return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
						new InputSource(new StringReader("<test type='custom'/>")));
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Document convertToDocument(Object object) {
			throw new UnsupportedOperationException();
		}

	}

}
