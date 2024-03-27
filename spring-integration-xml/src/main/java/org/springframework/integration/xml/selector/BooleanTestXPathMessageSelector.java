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

import java.util.Map;

import org.w3c.dom.Node;

import org.springframework.messaging.Message;
import org.springframework.xml.xpath.XPathExpression;

/**
 * Boolean XPath testing {@link org.springframework.integration.core.MessageSelector}.
 * Requires an XPathExpression
 * which can be evaluated using {@link XPathExpression#evaluateAsBoolean(Node)}.
 * Supports payloads of type {@link org.w3c.dom.Document} or {@link String}.
 *
 * @author Jonas Partner
 * @author Gary Russell
 */
public class BooleanTestXPathMessageSelector extends AbstractXPathMessageSelector {

	/**
	 * Create a boolean testing XPath {@link org.springframework.integration.core.MessageSelector}
	 * supporting multiple namespaces.
	 *
	 * @param expression XPath expression as a String
	 * @param namespaces Map of namespaces where the keys are namespace prefixes
	 */
	public BooleanTestXPathMessageSelector(String expression, Map<String, String> namespaces) {
		super(expression, namespaces);
	}

	/**
	 * Create a boolean testing XPath
	 * {@link org.springframework.integration.core.MessageSelector} supporting a single
	 * namespace.
	 *
	 * @param expression XPath expression as a String
	 * @param prefix namespace prefix
	 * @param namespace namespace URI
	 */
	public BooleanTestXPathMessageSelector(String expression, String prefix, String namespace) {
		super(expression, prefix, namespace);
	}

	/**
	 * Create a boolean testing XPath
	 * {@link org.springframework.integration.core.MessageSelector} with no namespace
	 * support.
	 *
	 * @param expression XPath expression as a String
	 */
	public BooleanTestXPathMessageSelector(String expression) {
		super(expression);
	}

	/**
	 * Create a boolean testing XPath
	 * {@link org.springframework.integration.core.MessageSelector} using the provided
	 * {@link XPathExpression}.
	 *
	 * @param expression XPath expression
	 */
	public BooleanTestXPathMessageSelector(XPathExpression expression) {
		super(expression);
	}

	/**
	 * Return true if the {@link XPathExpression} evaluates to <code>true</code>
	 */
	@Override
	public boolean accept(Message<?> message) {
		Node node = getConverter().convertToNode(message.getPayload());
		return getXPathExpresion().evaluateAsBoolean(node);
	}

}
