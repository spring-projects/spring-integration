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

package org.springframework.integration.xml.selector;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.selector.MessageSelector;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Base class for XPath {@link MessageSelector} implementations.
 * 
 * @author Jonas Partner
 */
public abstract class AbstractXPathMessageSelector implements MessageSelector {

	private final XPathExpression xPathExpresion;

	private XmlPayloadConverter converter = new DefaultXmlPayloadConverter();


	/**
	 * @param xPathExpression simple String expression
	 */
	public AbstractXPathMessageSelector(String xPathExpression) {
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression);
	}

	/**
	 * @param xPathExpression
	 * @param prefix
	 * @param namespace
	 */
	public AbstractXPathMessageSelector(String xPathExpression, String prefix, String namespace) {
		Map<String,String> namespaces = new HashMap<String, String>();
		namespaces.put(prefix, namespace);
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression, namespaces);
	}

	/**
	 * @param xPathExpression
	 * @param namespaces
	 */
	public AbstractXPathMessageSelector(String xPathExpression, Map<String,String> namespaces) {
		this.xPathExpresion = XPathExpressionFactory.createXPathExpression(xPathExpression, namespaces);
	}

	/**
	 * @param xPathExpression
	 */
	public AbstractXPathMessageSelector(XPathExpression xPathExpression) {
		this.xPathExpresion = xPathExpression;
	}


	/**
	 * Specify the converter used to convert payloads prior to XPath testing.
	 */
	public void setConverter(XmlPayloadConverter converter) {
		this.converter = converter;
	}

	protected XmlPayloadConverter getConverter() {
		return this.converter;
	}

	protected XPathExpression getXPathExpresion() {
		return xPathExpresion;
	}

}
