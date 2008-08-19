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
import org.springframework.integration.transformer.PayloadTransformer;
import org.springframework.integration.transformer.config.AbstractPayloadTransformerParser;
import org.springframework.integration.xml.transformer.XsltPayloadTransformer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class XsltPayloadTransformerParser extends AbstractPayloadTransformerParser {

	@Override
	protected Class<? extends PayloadTransformer<?, ?>> getTransformerClass() {
		return XsltPayloadTransformer.class; 
	}

	@Override
	protected void parsePayloadTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String xslResource = element.getAttribute("xsl-resource");
		String xslTemplates = element.getAttribute("xsl-templates");
		String resultTransformer = element.getAttribute("result-transformer");
		
		boolean bothHaveText = StringUtils.hasText(xslResource) && StringUtils.hasText(xslTemplates);
		boolean oneHasText = StringUtils.hasText(xslResource) || StringUtils.hasText(xslTemplates);
		Assert.state(!bothHaveText && oneHasText,
				"Exactly one of 'xsl-resource' or 'xsl-templates' is required.");

		if (StringUtils.hasText(xslResource)) {
			builder.addConstructorArgValue(xslResource);
		}
		else if (StringUtils.hasText(xslTemplates)) {
			builder.addConstructorArgReference(xslTemplates);
		}

		String sourceFactory = element.getAttribute("source-factory");
		if (StringUtils.hasText(sourceFactory)) {
			builder.addPropertyReference("sourceFactory", sourceFactory);
		}
		String resultFactory = element.getAttribute("result-factory");
		if (StringUtils.hasText(resultFactory)) {
			builder.addPropertyReference("resultFactory", resultFactory);
		}
		
		if(StringUtils.hasText(resultTransformer)){
			builder.addConstructorArgReference(resultTransformer);
		}
	}

}
