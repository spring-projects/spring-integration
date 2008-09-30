/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.integration.router.AbstractChannelNameResolver;
import org.springframework.integration.router.ChannelResolver;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Abstract base class for {@link ChannelResolver} classes that use
 * {@link XPathExpression} evaluation to determine channel names
 * @author Jonas Partner
 * 
 */
public abstract class AbstractXPathChannelNameResolver extends AbstractChannelNameResolver {

	private final XPathExpression xPathExpression;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();

	/**
	 * Creates an channel name resolver using an XPath expression which may
	 * contain zero or more namespace prefixes
	 * @param pathExpression
	 * @param namespaces
	 */
	public AbstractXPathChannelNameResolver(String pathExpression, Map<String, String> namespaces) {
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(pathExpression, namespaces);
	}

	/**
	 * Create a channel name resolver using an XPath expression with one
	 * namespace. For example expression '/ns1:one/@type' prefix 'ns1' namespace
	 * 'www.example.org'
	 * @param pathExpression
	 * @param prefix
	 * @param namespace
	 */
	public AbstractXPathChannelNameResolver(String pathExpression, String prefix, String namespace) {
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put(prefix, namespace);
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(pathExpression, namespaces);
	}

	/**
	 * Creates a channel name resolver using an XPath expression with no
	 * namespaces For example '/one/@type'
	 * @param pathExpression
	 */
	public AbstractXPathChannelNameResolver(String pathExpression) {
		this.xPathExpression = XPathExpressionFactory.createXPathExpression(pathExpression);
	}

	/**
	 * Creates a channel name resolver using the provided XPath expression
	 * @param pathExpression
	 */
	public AbstractXPathChannelNameResolver(XPathExpression pathExpression) {
		this.xPathExpression = pathExpression;
	}

	protected XmlPayloadConverter getConverter() {
		return converter;
	}

	/**
	 * Converter used to convert payloads prior to XPAth testing
	 * @param converter
	 */
	public void setConverter(XmlPayloadConverter converter) {
		this.converter = converter;
	}

	protected XPathExpression getXPathExpresion() {
		return xPathExpression;
	}

}
