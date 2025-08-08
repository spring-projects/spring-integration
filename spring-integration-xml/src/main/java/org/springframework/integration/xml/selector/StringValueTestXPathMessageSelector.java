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
 * XPath {@link org.springframework.integration.core.MessageSelector} that tests if a
 * provided value supports payloads of type {@link org.w3c.dom.Document} or
 * {@link String}.
 *
 * @author Jonas Partner
 * @author Gary Russell
 */
public class StringValueTestXPathMessageSelector extends AbstractXPathMessageSelector {

	private final String valueToTestFor;

	private volatile boolean caseSensitive = true;

	/**
	 * Create a selector which tests for the given value and supports multiple namespaces.
	 *
	 * @param expression XPath expression as a String
	 * @param namespaces Map of namespaces where the keys are namespace prefixes
	 * @param valueToTestFor value to test for
	 */
	public StringValueTestXPathMessageSelector(String expression, Map<String, String> namespaces, String valueToTestFor) {
		super(expression, namespaces);
		this.valueToTestFor = valueToTestFor;
	}

	/**
	 * Creates a single namespace Xpath selector.
	 *
	 * @param expression XPath expression as a String
	 * @param prefix namespace prefix
	 * @param namespace namespace URI
	 * @param valueToTestFor value to test for
	 */
	public StringValueTestXPathMessageSelector(String expression, String prefix, String namespace, String valueToTestFor) {
		super(expression, prefix, namespace);
		this.valueToTestFor = valueToTestFor;
	}

	/**
	 * Creates a non-namespaced testing selector.
	 *
	 * @param expression XPath expression as a String
	 * @param valueToTestFor value to test for
	 */
	public StringValueTestXPathMessageSelector(String expression, String valueToTestFor) {
		super(expression);
		this.valueToTestFor = valueToTestFor;
	}

	/**
	 * Creates a selector with the provided {@link XPathExpression}.
	 *
	 * @param expression XPath expression
	 * @param valueToTestFor value to test for
	 */
	public StringValueTestXPathMessageSelector(XPathExpression expression, String valueToTestFor) {
		super(expression);
		this.valueToTestFor = valueToTestFor;
	}

	/**
	 * Specify whether comparison of value returned by {@link XPathExpression}
	 * to test value should be case sensitive. Default is 'true'.
	 *
	 * @param caseSensitive true if the test should be case sensitive.
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Evaluate the payload and return true if the value returned by the
	 * {@link XPathExpression} is equal to the <code>valueToTestFor</code>.
	 */
	@Override
	public boolean accept(Message<?> message) {
		Node nodeToTest = getConverter().convertToNode(message.getPayload());
		String xPathResult = getXPathExpresion().evaluateAsString(nodeToTest);
		if (this.caseSensitive) {
			return this.valueToTestFor.equals(xPathResult);
		}
		else {
			return this.valueToTestFor.equalsIgnoreCase(xPathResult);
		}
	}

}
