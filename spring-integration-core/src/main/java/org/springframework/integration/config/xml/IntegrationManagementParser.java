/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.IntegrationManagementConfigurer;

/**
 * Parser for the &lt;management/&gt; element.
 *
 * @author Gary Russell
 * @since 4.2
 */
public class IntegrationManagementParser extends AbstractBeanDefinitionParser {


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		return IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(IntegrationManagementConfigurer.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-logging-enabled");
		return builder.getBeanDefinition();
	}

	@Override
	protected boolean shouldFireEvents() {
		return false;
	}

}
