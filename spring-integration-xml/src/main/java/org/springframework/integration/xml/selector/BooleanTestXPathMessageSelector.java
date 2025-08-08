/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
