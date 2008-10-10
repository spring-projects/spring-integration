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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.transformer.Transformer;
import org.springframework.integration.xml.transformer.XsltPayloadTransformer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class XsltPayloadTransformerParser extends AbstractTransformerParser {

	private ResultFactoryResultTypeHelper resultFactoryHelper = new ResultFactoryResultTypeHelper();


	@Override
	protected Class<? extends Transformer> getTransformerClass() {
		return XsltPayloadTransformer.class;
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String xslResource = element.getAttribute("xsl-resource");
		String xslTemplates = element.getAttribute("xsl-templates");
		String resultTransformer = element.getAttribute("result-transformer");
		String resultFactory = element.getAttribute("result-factory");
		String resultType = element.getAttribute("result-type");
		Assert.isTrue(StringUtils.hasText(xslResource) ^ StringUtils.hasText(xslTemplates),
				"Exactly one of 'xsl-resource' or 'xsl-templates' is required.");
		if (StringUtils.hasText(xslResource)) {
			builder.addConstructorArgValue(xslResource);
		}
		else if (StringUtils.hasText(xslTemplates)) {
			builder.addConstructorArgReference(xslTemplates);
		}
		resultFactoryHelper.assertResultFactoryAndTypeValid(resultFactory, resultType);
		resultFactoryHelper.addResultFactory(builder, resultType, resultFactory);
		if (StringUtils.hasText(resultTransformer)) {
			builder.addConstructorArgReference(resultTransformer);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "source-factory");
	}

}
