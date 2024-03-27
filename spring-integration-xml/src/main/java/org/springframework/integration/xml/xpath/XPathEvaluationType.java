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
