/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.xml.selector;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Base class for XPath {@link MessageSelector} implementations.
 *
 * @author Jonas Partner
 * @author Ngoc Nhan
 */
public abstract class AbstractXPathMessageSelector implements MessageSelector {

	private final XPathExpression xPathExpresion;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();

	/**
	 * @param xPathExpression XPath expression as a String
	 */
	public AbstractXPathMessageSelector(String xPathExpression) {
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression);
	}

	/**
	 * @param xPathExpression XPath expression as a String
	 * @param prefix namespace prefix
	 * @param namespace namespace URI
	 */
	public AbstractXPathMessageSelector(String xPathExpression, String prefix, String namespace) {
		Map<String, String> namespaces = new HashMap<>();
		namespaces.put(prefix, namespace);
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression, namespaces);
	}

	/**
	 * @param xPathExpression XPath expression as a String
	 * @param namespaces Map of namespaces with prefixes as the Map keys
	 */
	public AbstractXPathMessageSelector(String xPathExpression, Map<String, String> namespaces) {
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression, namespaces);
	}

	/**
	 * @param xPathExpression XPath expression
	 */
	public AbstractXPathMessageSelector(XPathExpression xPathExpression) {
		this.xPathExpresion = xPathExpression;
	}

	/**
	 * Specify the converter used to convert payloads prior to XPath testing.
	 *
	 * @param converter The payload converter.
	 */
	public void setConverter(XmlPayloadConverter converter) {
		this.converter = converter;
	}

	protected XmlPayloadConverter getConverter() {
		return this.converter;
	}

	protected XPathExpression getXPathExpresion() {
		return this.xPathExpresion;
	}

}
