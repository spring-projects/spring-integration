/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.xml.config;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.xml.selector.BooleanTestXPathMessageSelector;
import org.springframework.integration.xml.selector.StringValueTestXPathMessageSelector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Jonas Partner
 */
public class XPathSelectorParser extends AbstractSingleBeanDefinitionParser {

	private XPathExpressionParser xpathParser = new XPathExpressionParser();


	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String evaluationType = element.getAttribute("evaluation-result-type");
		String xPathExpressionRef = element.getAttribute("xpath-expression-ref");
		String stringTestValue = element.getAttribute("string-test-value");
		NodeList xPathExpressionNodes = element.getElementsByTagNameNS(element.getNamespaceURI(), "xpath-expression");
		Assert.isTrue(xPathExpressionNodes.getLength() < 2, "Only one xpath-expression child can be specified");
		boolean xPathExpressionChildPresent = xPathExpressionNodes.getLength() == 1;
		boolean xPathReferencePresent = StringUtils.hasText(xPathExpressionRef);
		Assert.isTrue(xPathExpressionChildPresent ^ xPathReferencePresent,
				"Exactly one of 'xpath-expression' or 'xpath-expression-ref' is required.");
		if (evaluationType.equals("boolean")) {
			builder.getBeanDefinition().setBeanClass(BooleanTestXPathMessageSelector.class);
			Assert.state(!StringUtils.hasText(stringTestValue),
					"'string-test-value' should not be specified when 'evaluation-result-type' is boolean");
		}
		else if (evaluationType.equals("string")) {
			Assert.hasText(stringTestValue,
					"'string-test-value' must be specified when 'evaluation-result-type' is string");
			builder.addConstructorArgValue(stringTestValue);
			builder.getBeanDefinition().setBeanClass(StringValueTestXPathMessageSelector.class);
		}
		else {
			throw new IllegalArgumentException("Unsupported value [" + evaluationType
					+ "] for 'evaluation-result-type', expected boolean or string.");
		}
		if (xPathExpressionChildPresent) {
			BeanDefinition beanDefinition = xpathParser.parse((Element) xPathExpressionNodes.item(0), parserContext);
			builder.addConstructorArgValue(beanDefinition);
		}
		else {
			builder.addConstructorArgReference(xPathExpressionRef);
		}
	}

}
