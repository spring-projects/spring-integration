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

import java.util.Map;

import org.springframework.integration.message.Message;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.xml.xpath.XPathExpression;
import org.w3c.dom.Node;

/**
 * Evaluates the payload using {@link XPathExpression#evaluateAsString(Node)} to
 * extract a channel name. The payload is extracted as a node using the provided
 * {@link XmlPayloadConverter} with {@link DefaultXmlPayloadConverter} being the
 * default
 * @author Jonas Partner
 */
public class XPathSingleChannelNameResolver extends AbstractXPathChannelNameResolver {

	/**
	 * @see AbstractXPathChannelNameResolver#AbstractXPathChannelNameResolver(String,
	 * Map)
	 */
	public XPathSingleChannelNameResolver(String pathExpression, Map<String, String> namespaces) {
		super(pathExpression, namespaces);
	}

	/**
	 * @see AbstractXPathChannelNameResolver#AbstractXPathChannelNameResolver(String,
	 * String, String)
	 */
	public XPathSingleChannelNameResolver(String pathExpression, String prefix, String namespace) {
		super(pathExpression, prefix, namespace);
	}

	/**
	 * @see AbstractXPathChannelNameResolver#AbstractXPathChannelNameResolver(String)
	 */
	public XPathSingleChannelNameResolver(String pathExpression) {
		super(pathExpression);
	}

	/**
	 * @see AbstractXPathChannelNameResolver#AbstractXPathChannelNameResolver(XPathExpression)
	 */
	public XPathSingleChannelNameResolver(XPathExpression pathExpression) {
		super(pathExpression);
	}

	public String[] resolveChannelNames(Message<?> message) {
		Node node = getConverter().convertToNode(message.getPayload());
		return new String[] { getXPathExpresion().evaluateAsString(node) };
	}

}
