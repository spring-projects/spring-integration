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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xml.xpath.XPathExpression;

/**
 * XPath {@link org.springframework.integration.core.MessageSelector} that tests if a
 * provided String value matches a given Regular Expression.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class RegexTestXPathMessageSelector extends AbstractXPathMessageSelector {

	private static final String REGEX_MUST_NOT_BE_NULL = "regex must not be null";

	private final String regex;

	/**
	 * Creates a selector which attempts to match the given regex and supports multiple namespaces.
	 *
	 * @param expression XPath expression as a String
	 * @param namespaces Map of namespaces where the keys are namespace prefixes
	 * @param regex regular expression to match
	 */
	public RegexTestXPathMessageSelector(String expression, Map<String, String> namespaces, String regex) {
		super(expression, namespaces);
		Assert.notNull(regex, REGEX_MUST_NOT_BE_NULL);
		this.regex = regex;
	}

	/**
	 * Creates a selector which attempts to match the given regex and supports a single namespace.
	 *
	 * @param expression XPath expression as a String
	 * @param prefix namespace prefix
	 * @param namespace namespace URI
	 * @param regex regular expression to match
	 */
	public RegexTestXPathMessageSelector(String expression, String prefix, String namespace, String regex) {
		super(expression, prefix, namespace);
		Assert.notNull(regex, REGEX_MUST_NOT_BE_NULL);
		this.regex = regex;
	}

	/**
	 * Creates a non-namespaced selector which attempts to match the given regex.
	 *
	 * @param expression XPath expression as a String
	 * @param regex regular expression to match
	 */
	public RegexTestXPathMessageSelector(String expression, String regex) {
		super(expression);
		Assert.notNull(regex, REGEX_MUST_NOT_BE_NULL);
		this.regex = regex;
	}

	/**
	 * Creates a selector which attempts to match the given regex against the evaluation result
	 * of the provided {@link XPathExpression}.
	 *
	 * @param expression XPath expression
	 * @param regex regular expression to match
	 */
	public RegexTestXPathMessageSelector(XPathExpression expression, String regex) {
		super(expression);
		Assert.notNull(regex, REGEX_MUST_NOT_BE_NULL);
		this.regex = regex;
	}

	/**
	 * Evaluate the payload and return true if the value returned by the
	 * {@link XPathExpression} matches the <code>regex</code>.
	 */
	@Override
	public boolean accept(Message<?> message) {
		Node nodeToTest = getConverter().convertToNode(message.getPayload());
		String xPathResult = getXPathExpresion().evaluateAsString(nodeToTest);
		return StringUtils.hasText(xPathResult) && xPathResult.matches(this.regex);
	}

}
