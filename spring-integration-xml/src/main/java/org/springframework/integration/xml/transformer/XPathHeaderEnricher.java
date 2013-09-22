/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import java.util.Map;

import org.w3c.dom.Node;

import org.springframework.integration.Message;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.integration.xml.xpath.XPathEvaluationType;
import org.springframework.util.Assert;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

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
	 */
	public XPathHeaderEnricher(Map<String, XPathExpressionEvaluatingHeaderValueMessageProcessor> expressionMap) {
		super(expressionMap);
	}


	static class XPathExpressionEvaluatingHeaderValueMessageProcessor implements HeaderValueMessageProcessor<Object> {

		private final XPathExpression expression;

		private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();

		private volatile  XPathEvaluationType evaluationType = XPathEvaluationType.STRING_RESULT;

		private volatile Boolean overwrite = null;


		public XPathExpressionEvaluatingHeaderValueMessageProcessor(String expression) {
			Assert.hasText(expression, "expression must have text");
			this.expression = XPathExpressionFactory.createXPathExpression(expression);
		}

		public XPathExpressionEvaluatingHeaderValueMessageProcessor(XPathExpression expression) {
			Assert.notNull(expression, "expression must not be null");
			this.expression = expression;
		}

		public void setEvaluationType(XPathEvaluationType evaluationType) {
			this.evaluationType = evaluationType;
		}

		public void setOverwrite(Boolean overwrite) {
			this.overwrite = overwrite;
		}

		public Boolean isOverwrite() {
			return this.overwrite;
		}

		public Object processMessage(Message<?> message) {
			Node node = converter.convertToNode(message.getPayload());
			Object result = this.evaluationType.evaluateXPath(this.expression, node);
			if (result instanceof String && ((String) result).length() == 0) {
				result = null;
			}
			return result;
		}
	}

}
