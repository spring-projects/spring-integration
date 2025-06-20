/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.xml.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'xpath-transformer' element.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class XPathTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return "org.springframework.integration.xml.transformer.XPathTransformer";
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String expression = element.getAttribute("xpath-expression");
		String expressionRef = element.getAttribute("xpath-expression-ref");
		boolean hasRef = StringUtils.hasText(expressionRef);
		Assert.isTrue(hasRef ^ StringUtils.hasText(expression),
				"Exactly one of the 'xpath-expression' or 'xpath-expression-ref' attributes is required.");
		if (hasRef) {
			builder.addConstructorArgReference(expressionRef);
		}
		else {
			builder.addConstructorArgValue(expression);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "evaluation-type");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "node-mapper");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "converter");
	}

}
