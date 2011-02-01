/*
 * Copyright 2002-2009 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;service-activator&gt; element.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ServiceActivatorParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinition innerHandlerDefinition = this.parseInnerHandlerDefinition(element, parserContext);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".handler.ServiceActivatingHandler");
		if (innerHandlerDefinition != null){
			builder.addConstructorArgValue(innerHandlerDefinition);
		}
		else {
			String ref = element.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(ref)) {
				parserContext.getReaderContext().error("The '" + REF_ATTRIBUTE + "' attribute is required for element "
						+ IntegrationNamespaceUtils.createElementDescription(element) + ".", element);
			}
			builder.addConstructorArgReference(ref);
		}
		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			builder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(method, "java.lang.String");
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		return builder;
	}

}
