/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.integration.xml.selector.XmlValidatingMessageSelector;
import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.FilterFactoryBean;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class XmlPayloadValidatingFilterParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(FilterFactoryBean.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(filterBuilder, element, "discard-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(filterBuilder, element, "throw-exception-on-rejection");
		BeanDefinitionBuilder selectorBuilder = BeanDefinitionBuilder.genericBeanDefinition(XmlValidatingMessageSelector.class);
		String validator = element.getAttribute("xml-validator");
		String schemaLocation = element.getAttribute("schema-location");
		boolean validatorDefined = StringUtils.hasText(validator);
		boolean schemaLocationDefined = StringUtils.hasText(schemaLocation);
		if (!(validatorDefined ^ schemaLocationDefined)) {
			throw new BeanDefinitionStoreException(
					"Exactly one of 'xml-validator' or 'schema-location' is allowed on the 'validating-filter' element");
		}
		if (schemaLocationDefined) {
			selectorBuilder.addConstructorArgValue(schemaLocation);
			// it is a restriction with the default value of 'xml-schema' which
			// corresponds to 'http://www.w3.org/2001/XMLSchema'
			String schemaType = element.getAttribute("schema-type");
			selectorBuilder.addConstructorArgValue(schemaType);
		}
		else {
			selectorBuilder.addConstructorArgReference(validator);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(selectorBuilder, element, "throw-exception-on-rejection");
		filterBuilder.addPropertyValue("targetObject", selectorBuilder.getBeanDefinition());
		return filterBuilder;
	}

}
