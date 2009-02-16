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

import javax.xml.XMLConstants;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.xml.router.SchemaValidator;
import org.springframework.integration.xml.router.XmlPayloadValidatingRouter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Jonas Partner
 */
public class XmlPayloadValidatingRouterParser extends
		AbstractConsumerEndpointParser {

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
	protected BeanDefinitionBuilder parseHandler(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition();
		builder.getBeanDefinition().setBeanClass(
				XmlPayloadValidatingRouter.class);
		String channelResolver = element.getAttribute("channel-resolver");

		String validChannelName = element.getAttribute("valid-channel");
		String invalidChannelName = element.getAttribute("invalid-channel");
		String schemaType = element.getAttribute("schema-type");
		String schemaLocation = element.getAttribute("schema-location");	
			
		Assert.state(schemaType.equals("xml-schema")
				|| schemaType.equals("relax-ng"), "Unrecognised schema type "
				+ schemaType);

	
		Assert.state(StringUtils.hasText(invalidChannelName)
				&& StringUtils.hasText(validChannelName),
				"valid-channel and invalid-channel must both be specified");

		builder.addConstructorArgValue(validChannelName);
		builder.addConstructorArgValue(invalidChannelName);
	
		
		
		BeanDefinition validatorBeanDefinition; 
		if (schemaType.equals("xml-schema")) {
			validatorBeanDefinition = createValidator(XMLConstants.W3C_XML_SCHEMA_NS_URI, schemaLocation);
		} else  {
			validatorBeanDefinition = createValidator(XMLConstants.RELAXNG_NS_URI, schemaLocation);
		}
		builder.addConstructorArgValue(validatorBeanDefinition);
		

		if (StringUtils.hasText(channelResolver)) {
			builder.addPropertyReference("channelResolver", channelResolver);
		}

		return builder;
	}
	
	protected BeanDefinition createValidator(String schemaType, String schemaLocation){
		BeanDefinitionBuilder xmlValidator = BeanDefinitionBuilder
		.genericBeanDefinition();
		xmlValidator.getBeanDefinition().setBeanClass(SchemaValidator.class);
		xmlValidator.addConstructorArgValue(schemaLocation);
		xmlValidator.addConstructorArgValue(schemaType);
		
		return xmlValidator.getBeanDefinition();
	}

}
