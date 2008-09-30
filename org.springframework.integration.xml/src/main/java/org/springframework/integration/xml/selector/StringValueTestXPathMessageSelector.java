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
package org.springframework.integration.xml.selector;

import java.util.Map;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.xml.xpath.XPathExpression;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * XPath {@link MessageSelector} which tests for a provided value Supports
 * payloads of type {@link Document} or {@link String}
 * @author Jonas Partner
 * 
 */
public class StringValueTestXPathMessageSelector extends AbstractXPathMessageSelector {

	private final String valueToTestFor;

	private volatile boolean caseSensitive = true;

	/**
	 * Create a selector which tests for the given value and supports multiple
	 * namespaces
	 * @param pathExpression
	 * @param namespaces
	 * @param valueToTestFor
	 */
	public StringValueTestXPathMessageSelector(String pathExpression, Map<String, String> namespaces,
			String valueToTestFor) {
		super(pathExpression, namespaces);
		this.valueToTestFor = valueToTestFor;
	}

	/**
	 * Creates a single namespace Xpath selector
	 * @param pathExpression
	 * @param prefix
	 * @param namespace
	 * @param valueToTestFor
	 */
	public StringValueTestXPathMessageSelector(String pathExpression, String prefix, String namespace,
			String valueToTestFor) {
		super(pathExpression, prefix, namespace);
		this.valueToTestFor = valueToTestFor;
	}

	/**
	 * Creates non namespaced testing selector
	 * @param pathExpression
	 * @param valueToTestFor
	 */
	public StringValueTestXPathMessageSelector(String pathExpression, String valueToTestFor) {

		super(pathExpression);
		this.valueToTestFor = valueToTestFor;
	}

	/**
	 * Creates selector with provided {@link XPathExpression}
	 * @param pathExpression
	 * @param valueToTestFor
	 */
	public StringValueTestXPathMessageSelector(XPathExpression pathExpression, String valueToTestFor) {
		super(pathExpression);
		this.valueToTestFor = valueToTestFor;
	}

	/**
	 * Evaluate the payload returning true if the value returned by the
	 * {@link XPathExpression} is equal to the valueToTestFor
	 */
	public boolean accept(Message<?> message) {
		Node nodeToTest = getConverter().convertToNode(message.getPayload());
		String xPathResult = getXPathExpresion().evaluateAsString(nodeToTest);
		if (caseSensitive) {
			return valueToTestFor.equals(xPathResult);
		}
		else {
			return valueToTestFor.equalsIgnoreCase(xPathResult);
		}
	}

	/**
	 * should comparison of value returned by {@link XPathExpression} to test
	 * value be case sensitive
	 * @param caseSensitive
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

}
