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

package org.springframework.integration.xml.router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import org.springframework.integration.Message;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.xml.xpath.NodeMapper;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Message Router that uses {@link XPathExpression} evaluation to determine channel names.
 * 
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 */
public class XPathRouter extends AbstractMessageRouter {

	private volatile NodeMapper nodeMapper = new TextContentNodeMapper();

	private final XPathExpression xPathExpression;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();


	/**
	 * Create a router that uses an XPath expression. The expression may
	 * contain zero or more namespace prefixes.
	 * 
	 * @param expression the XPath expression as a String
	 * @param namespaces map of namespaces with prefixes as the map keys
	 */
	public XPathRouter(String expression, Map<String, String> namespaces) {
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(expression, namespaces);
	}

	/**
	 * Create a router uses an XPath expression with one namespace. For example,
	 * expression='/ns1:one/@type' prefix='ns1' namespace='www.example.org'
	 * 
	 * @param expression the XPath expression as a String
	 * @param prefix namespace prefix
	 * @param namespace namespace uri
	 */
	public XPathRouter(String expression, String prefix, String namespace) {
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put(prefix, namespace);
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(expression, namespaces);
	}

	/**
	 * Create a router that uses an XPath expression with no namespaces.
	 * For example '/one/@type'
	 * 
	 * @param expression the XPath expression as a String
	 */
	public XPathRouter(String expression) {
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(expression);
	}

	/**
	 * Create a router that uses the provided XPath expression.
	 * 
	 * @param expression the XPath expression
	 */
	public XPathRouter(XPathExpression expression) {
		this.xPathExpression = expression;
	}


	protected XmlPayloadConverter getConverter() {
		return this.converter;
	}

	/**
	 * Specify the Converter to use when converting payloads prior to XPath evaluation.
	 */
	public void setConverter(XmlPayloadConverter converter) {
		this.converter = converter;
	}

	protected XPathExpression getXPathExpression() {
		return this.xPathExpression;
	}

	public String getComponentType() {
		return "xml:xpath-router";
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object> getChannelIdentifiers(Message<?> message) {
		Node node = getConverter().convertToNode(message.getPayload());
		return getXPathExpression().evaluate(node, this.nodeMapper);
	}


	private static class TextContentNodeMapper implements NodeMapper {

		public Object mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent();
		}
	}

}
