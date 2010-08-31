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

package org.springframework.integration.config.xml;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class MessageHistoryParser extends AbstractSimpleBeanDefinitionParser {

	private static final String CONFIGURER_CLASSNAME = "org.springframework.integration.history.MessageHistoryConfigurer";


	@Override
	protected String getBeanClassName(Element element) {
		return CONFIGURER_CLASSNAME;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		if (parserContext.getRegistry().containsBeanDefinition(CONFIGURER_CLASSNAME)) {
			throw new BeanDefinitionStoreException("At most one MessageHistoryConfigurer may be registered within a context.");
		}
		return CONFIGURER_CLASSNAME;
	}
	
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
		
	}
	
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "tracked-components", "componentNamePatterns");
		postProcess(builder, element);
	}

}
