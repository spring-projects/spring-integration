/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelNameResolvingRouterParser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parser for the &lt;xpath-router/&gt; element.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class XPathRouterParser extends AbstractChannelNameResolvingRouterParser {

	private XPathExpressionParser xpathParser = new XPathExpressionParser();

	@Override
	protected BeanDefinition doParseRouter(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder xpathRouterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.xml.router.XPathRouter");
		NodeList xPathExpressionNodes = element.getElementsByTagNameNS(
				element.getNamespaceURI(), "xpath-expression");
		Assert.isTrue(xPathExpressionNodes.getLength() < 2, "Only one xpath-expression child can be specified.");
		String xPathExpressionRef = element.getAttribute("xpath-expression-ref");
		boolean xPathExpressionChildPresent = (xPathExpressionNodes.getLength() == 1);
		boolean xPathReferencePresent = StringUtils.hasText(xPathExpressionRef);
		Assert.isTrue(xPathExpressionChildPresent ^ xPathReferencePresent,
				"Exactly one of 'xpath-expression' or 'xpath-expression-ref' is required.");
		if (xPathExpressionChildPresent) {
			BeanDefinition beanDefinition = this.xpathParser.parse(
					(Element) xPathExpressionNodes.item(0), parserContext);
			xpathRouterBuilder.addConstructorArgValue(beanDefinition);
		}
		else { 
			xpathRouterBuilder.addConstructorArgReference(xPathExpressionRef);
		}
		return xpathRouterBuilder.getBeanDefinition();
	}

}
