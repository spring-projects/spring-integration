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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 */
public class XmlPayloadValidatingFilterParser extends AbstractConsumerEndpointParser {
	private static String SELECTOR = 
			"org.springframework.integration.xml.selector.XmlValidatingMessageSelector";
	private static String FILTER = 
		"org.springframework.integration.config.FilterFactoryBean";
	
	/** Constant that defines a W3C XML Schema. */
    public static final String SCHEMA_W3C_XML = "http://www.w3.org/2001/XMLSchema";

    /** Constant that defines a RELAX NG Schema. */
    public static final String SCHEMA_RELAX_NG = "http://relaxng.org/ns/structure/1.0";

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(FILTER);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(filterBuilder, element, "discard-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(filterBuilder, element, "throw-exception-on-rejection");
		
		BeanDefinitionBuilder selectorBuilder = BeanDefinitionBuilder.genericBeanDefinition(SELECTOR);
		String validator = element.getAttribute("xml-validator");
		String schemaLocation = element.getAttribute("schema-location");
		boolean validatorDefined = StringUtils.hasText(validator);
		boolean schemaLocationDefined = StringUtils.hasText(schemaLocation);
		selectorBuilder.addPropertyValue("throwExceptionOnRejection", element.getAttribute("throw-exception-on-rejection"));
			
		if (!(validatorDefined ^ schemaLocationDefined)) {
			throw new BeanDefinitionStoreException("Exactly one of 'xml-validator' or 'schema-location' is allowed on the 'validating-filter' element");
		}
		if (schemaLocationDefined){
			selectorBuilder.addConstructorArgValue(schemaLocation);
			String schemaType = "xml-schema".equals(element.getAttribute("schema-type")) ? SCHEMA_W3C_XML : SCHEMA_RELAX_NG;;	
			selectorBuilder.addConstructorArgValue(schemaType);
		} 
		else {
			selectorBuilder.addConstructorArgReference(validator);
		}
		
		filterBuilder.addPropertyValue("targetObject", selectorBuilder.getBeanDefinition());
		return filterBuilder;
	}
	
	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}
}
