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

/**
 * Boolean XPath testing {@link MessageSelector}. Requires an XPathExpression
 * which can be evaluated using {@link XPathExpression} evaluateAsBoolean.
 * Supports payloads of type {@link Document} or {@link String}
 * @author Jonas Partner
 * 
 */
public class BooleanTestXPathMessageSelector extends AbstractXPathMessageSelector {

	/**
	 * Create a boolean testing XPath {@link MessageSelector} supporting
	 * mutliple namespaces
	 * @param pathExpression
	 * @param namespaces
	 */
	public BooleanTestXPathMessageSelector(String pathExpression, Map<String, String> namespaces) {
		super(pathExpression, namespaces);
	}

	/**
	 * Create a boolean testing XPath {@link MessageSelector} supporting a
	 * single namespace
	 * @param pathExpression
	 * @param prefix
	 * @param namespace
	 */
	public BooleanTestXPathMessageSelector(String pathExpression, String prefix, String namespace) {
		super(pathExpression, prefix, namespace);
	}

	/**
	 * Creates a boolean testing XPath {@link MessageSelector} with no namespace
	 * support
	 * @param pathExpression
	 */
	public BooleanTestXPathMessageSelector(String pathExpression) {
		super(pathExpression);
	}

	/**
	 * Creates a boolean testing XPath {@link MessageSelector} using the
	 * provided {@link XPathExpression}
	 * @param pathExpression
	 */
	public BooleanTestXPathMessageSelector(XPathExpression pathExpression) {
		super(pathExpression);
	}

	/**
	 * return true if the {@link XPathExpression} evaluates to <code>true</code>
	 */
	public boolean accept(Message<?> message) {
		Document doc = getConverter().convertToDocument(message.getPayload());
		return getXPathExpresion().evaluateAsBoolean(doc);
	}
}
