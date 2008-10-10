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

package org.springframework.integration.xml.router;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.router.AbstractChannelMappingMessageRouter;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Abstract base class for Message Routers that use
 * {@link XPathExpression} evaluation to determine channel names.
 * 
 * @author Jonas Partner
 */
public abstract class AbstractXPathRouter extends AbstractChannelMappingMessageRouter {

	private final XPathExpression xPathExpression;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();


	/**
	 * Create a router that uses an XPath expression. The expression may
	 * contain zero or more namespace prefixes.
	 * 
	 * @param expression
	 * @param namespaces
	 */
	public AbstractXPathRouter(String expression, Map<String, String> namespaces) {
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(expression, namespaces);
	}

	/**
	 * Create a router uses an XPath expression with one namespace. For example,
	 * expression='/ns1:one/@type' prefix='ns1' namespace='www.example.org'
	 * 
	 * @param expression
	 * @param prefix
	 * @param namespace
	 */
	public AbstractXPathRouter(String expression, String prefix, String namespace) {
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put(prefix, namespace);
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(expression, namespaces);
	}

	/**
	 * Create a router that uses an XPath expression with no namespaces.
	 * For example '/one/@type'
	 * 
	 * @param expression
	 */
	public AbstractXPathRouter(String expression) {
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(expression);
	}

	/**
	 * Create a router that uses the provided XPath expression.
	 * 
	 * @param expression
	 */
	public AbstractXPathRouter(XPathExpression expression) {
		this.xPathExpression = expression;
	}


	protected XmlPayloadConverter getConverter() {
		return this.converter;
	}

	/**
	 * Converter used to convert payloads prior to XPath testing.
	 * 
	 * @param converter
	 */
	public void setConverter(XmlPayloadConverter converter) {
		this.converter = converter;
	}

	protected XPathExpression getXPathExpression() {
		return this.xPathExpression;
	}

}
