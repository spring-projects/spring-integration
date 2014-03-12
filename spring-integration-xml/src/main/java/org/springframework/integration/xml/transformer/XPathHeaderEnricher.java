/*
 * Copyright 2002-2014 the original author or authors.
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
