/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.xml.transformer.XsltPayloadTransformer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * 
 * @author Jonas Partner
 *
 */
public class XsltPayloadTransformerParser extends AbstractSingleBeanDefinitionParser {

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
		String xslResource = element.getAttribute("xsl-resource");
		String xslTemplates = element.getAttribute("xsl-templates");
		
		boolean bothHaveText = StringUtils.hasText(xslResource) && StringUtils.hasText(xslTemplates);
		boolean oneHasText = StringUtils.hasText(xslResource) || StringUtils.hasText(xslTemplates);
		
		Assert.state(!bothHaveText && oneHasText,
				"Exaclty one of xsl-resource or xsl-templates should be specified");

		builder.getBeanDefinition().setBeanClass(XsltPayloadTransformer.class);

		
		if(StringUtils.hasText(xslResource)){
		builder.getBeanDefinition().getConstructorArgumentValues()
				.addGenericArgumentValue(new ValueHolder(xslResource));
		} else if (StringUtils.hasText(xslTemplates)){
			builder.getBeanDefinition().getConstructorArgumentValues()
			.addGenericArgumentValue(new RuntimeBeanReference(xslTemplates));	
		}
		
		String sourceFactory = element.getAttribute("source-factory");
		if(StringUtils.hasText(sourceFactory)){
			builder.getBeanDefinition().getPropertyValues().addPropertyValue("sourceFactory", new RuntimeBeanReference(sourceFactory));
		}
		
		String resultFactory = element.getAttribute("result-factory");
		if(StringUtils.hasText(resultFactory)){
			builder.getBeanDefinition().getPropertyValues().addPropertyValue("resultFactory", new RuntimeBeanReference(resultFactory));
		}
	}

}
