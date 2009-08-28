/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;splitter/&gt; element.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class SplitterParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".config.SplitterFactoryBean");

		BeanDefinition innerDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		String ref = element.getAttribute(REF_ATTRIBUTE);
		String expression = element.getAttribute(EXPRESSION_ATTRIBUTE);
		boolean hasRef = StringUtils.hasText(ref);
		boolean hasExpression = StringUtils.hasText(expression);

		if (innerDefinition != null) {
			if (hasRef || hasExpression) {
				parserContext.getReaderContext().error(
						"Neither 'ref' nor 'expression' are permitted when an inner bean (<bean/>) is configured.", element);
				return null;
			}
			builder.addPropertyValue("targetObject", innerDefinition);
		}
		else if (hasRef) {
			builder.addPropertyReference("targetObject", ref);
		}
		else if (hasExpression) {
			builder.addPropertyValue("expression", expression);
		}
		else {
			// will create a DefaultSplitter
			return builder;
		}

		String method = element.getAttribute(METHOD_ATTRIBUTE);
		if (StringUtils.hasText(method)) {
			if (hasExpression) {
				parserContext.getReaderContext().error(
						"A 'method' attribute is not permitted when configuring an 'expression'.", element);
			}
			builder.addPropertyValue("targetMethodName", method);
		}
		return builder;
	}

}
