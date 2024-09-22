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

package org.springframework.integration.xml.config;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Mike Bazos
 * @author liujiong
 * @author Gary Russell
 * @author Ngoc Nhan
 */
public class XsltPayloadTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return "org.springframework.integration.xml.transformer.XsltPayloadTransformer";
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String xslResource = element.getAttribute("xsl-resource");
		String xslTemplates = element.getAttribute("xsl-templates");
		String resultTransformer = element.getAttribute("result-transformer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "result-type");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "result-factory", "resultFactoryName");
		String transformerFactoryClass = element.getAttribute("transformer-factory-class");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "xslt-param-headers");
		Assert.isTrue(StringUtils.hasText(xslResource) ^ StringUtils.hasText(xslTemplates),
				"Exactly one of 'xsl-resource' or 'xsl-templates' is required.");
		if (StringUtils.hasText(xslResource)) {
			builder.addConstructorArgValue(xslResource);
		}
		else if (StringUtils.hasText(xslTemplates)) {
			builder.addConstructorArgReference(xslTemplates);
		}
		if (StringUtils.hasText(resultTransformer)) {
			builder.addConstructorArgReference(resultTransformer);
		}
		if (StringUtils.hasText(transformerFactoryClass)) {
			builder.addConstructorArgValue(transformerFactoryClass);
		}
		List<Element> xslParameterElements = DomUtils.getChildElementsByTagName(element, "xslt-param");
		if (!CollectionUtils.isEmpty(xslParameterElements)) {
			Map<String, Object> xslParameterMappings = new ManagedMap<>();
			for (Element xslParameterElement : xslParameterElements) {
				String name = xslParameterElement.getAttribute("name");
				String expression = xslParameterElement.getAttribute("expression");
				String value = xslParameterElement.getAttribute("value");
				Assert.isTrue(StringUtils.hasText(expression) ^ StringUtils.hasText(value),
						"Exactly one of 'expression' or 'value' is required.");
				RootBeanDefinition expressionDef = null;
				if (StringUtils.hasText(value)) {
					expressionDef = new RootBeanDefinition("org.springframework.expression.common.LiteralExpression");
					expressionDef.getConstructorArgumentValues().addGenericArgumentValue(value);
				}
				else if (StringUtils.hasText(expression)) {
					expressionDef = new RootBeanDefinition("org.springframework.integration.config.ExpressionFactoryBean");
					expressionDef.getConstructorArgumentValues().addGenericArgumentValue(expression);
				}
				if (expressionDef != null) {
					xslParameterMappings.put(name, expressionDef);
				}
			}
			builder.addPropertyValue("xslParameterMappings", xslParameterMappings);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "source-factory");

	}

}
