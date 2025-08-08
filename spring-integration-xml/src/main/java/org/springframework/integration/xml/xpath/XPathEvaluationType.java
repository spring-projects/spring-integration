/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.xpath;

import org.w3c.dom.Node;

import org.springframework.xml.xpath.XPathExpression;

/**
 * Enumeration of different types of XPath evaluation used to indicate the type
 * of evaluation that should be carried out using a provided XPath expression.
 *
 * @author Mark Fisher
 * @author Jonas Partner
 */
public enum XPathEvaluationType {

	BOOLEAN_RESULT {
		public Object evaluateXPath(XPathExpression expression, Node node) {
			return expression.evaluateAsBoolean(node);
		}
	},

	STRING_RESULT {
		public Object evaluateXPath(XPathExpression expression, Node node) {
			return expression.evaluateAsString(node);
		}
	},

	NUMBER_RESULT {
		public Object evaluateXPath(XPathExpression expression, Node node) {
			return expression.evaluateAsNumber(node);
		}
	},

	NODE_RESULT {
		public Object evaluateXPath(XPathExpression expression, Node node) {
			return expression.evaluateAsNode(node);
		}
	},

	NODE_LIST_RESULT {
		public Object evaluateXPath(XPathExpression expression, Node node) {
			return expression.evaluateAsNodeList(node);
		}
	};

	public abstract Object evaluateXPath(XPathExpression expression, Node node);

}
