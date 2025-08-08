/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.transformer;

import java.util.Map;

import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.xml.transformer.support.XPathExpressionEvaluatingHeaderValueMessageProcessor;

/**
 * Transformer implementation that evaluates XPath expressions against the
 * message payload and inserts the result of the evaluation into a message
 * header. The header names will match the keys in the map of expressions.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @since 2.0
 */
public class XPathHeaderEnricher extends HeaderEnricher {

	/**
	 * Create an instance of XPathHeaderEnricher using a map with header names as keys
	 * and XPathExpressionValueHolders to evaluate the values.
	 *
	 * @param expressionMap The expression map.
	 */
	public XPathHeaderEnricher(Map<String, XPathExpressionEvaluatingHeaderValueMessageProcessor> expressionMap) {
		super(expressionMap);
	}

}
