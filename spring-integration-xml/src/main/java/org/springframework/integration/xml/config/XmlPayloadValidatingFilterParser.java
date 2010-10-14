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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;

/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 */
public class XmlPayloadValidatingFilterParser extends AbstractConsumerEndpointParser {
	private static String SELECTOR = 
			"org.springframework.integration.xml.selector.SchemaValidatingMessageSelector";
	private static String FILTER = 
		"org.springframework.integration.config.FilterFactoryBean";

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(FILTER);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(filterBuilder, element, "discard-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(filterBuilder, element, "throw-exception-on-rejection");
		
		BeanDefinitionBuilder selectorBuilder = BeanDefinitionBuilder.genericBeanDefinition(SELECTOR);	
		selectorBuilder.addConstructorArgValue(element.getAttribute("schema-location"));
		selectorBuilder.addPropertyValue("schemaType", element.getAttribute("schema-type"));
		
		filterBuilder.addPropertyValue("targetObject", selectorBuilder.getBeanDefinition());
		return filterBuilder;
	}
}
