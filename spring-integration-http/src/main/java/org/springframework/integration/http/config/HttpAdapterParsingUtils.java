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

package org.springframework.integration.http.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0.2
 */
abstract class HttpAdapterParsingUtils {

	static final String[] REST_TEMPLATE_REFERENCE_ATTRIBUTES = {
		"request-factory", "error-handler", "message-converters"
	};

	static void verifyNoRestTemplateAttributes(Element element, ParserContext parserContext) {
		for (String attributeName : REST_TEMPLATE_REFERENCE_ATTRIBUTES) {
			if (element.hasAttribute(attributeName)) {
				parserContext.getReaderContext().error("When providing a 'rest-template' reference, the '"
						+ attributeName + "' attribute is not allowed.",
						parserContext.extractSource(element));
			}
		}
	}

	static void configureUriVariableExpressions(BeanDefinitionBuilder builder, Element element) {
		List<Element> uriVariableElements = DomUtils.getChildElementsByTagName(element, "uri-variable");
		if (!CollectionUtils.isEmpty(uriVariableElements)) {
			ManagedMap<String, Object> uriVariableExpressions = new ManagedMap<String, Object>();
			for (Element uriVariableElement : uriVariableElements) {
				String name = uriVariableElement.getAttribute("name");
				String expression = uriVariableElement.getAttribute("expression");
				BeanDefinitionBuilder factoryBeanBuilder = BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.http.outbound.UriVariableExpressionDelegateFactoryBean");
				factoryBeanBuilder.addConstructorArgValue(expression);
				factoryBeanBuilder.addConstructorArgValue(uriVariableElement.getAttribute("encode"));
				uriVariableExpressions.put(name,  factoryBeanBuilder.getBeanDefinition());
			}
			builder.addPropertyValue("uriVariableExpressions", uriVariableExpressions);
		}
	}

}
