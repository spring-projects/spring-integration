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
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;filter/&gt; element.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class FilterParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".filter.MessageFilter");
		
		builder.addConstructorArgReference((String) this.parseSelector(element, parserContext));

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "discard-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "throw-exception-on-rejection");
		return builder;
	}

	private String parseSelector(Element element, ParserContext parserContext) {
		BeanDefinition innerHandlerDefinition = this.parseInnerHandlerDefinition(element, parserContext);
		String ref = null;
		if (innerHandlerDefinition == null){
			ref = element.getAttribute("ref");
			if (!StringUtils.hasText(ref)) {
				parserContext.getReaderContext().error("Either \"ref\" attribute or inner bean (<bean/>) definition of concrete implementation of " +
														"this MessageFilter is required.", element);
				return null;
			}
		}
		String method = element.getAttribute("method");
		if (!StringUtils.hasText(method)) {
			return ref;
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".filter.MethodInvokingSelector");
		if (innerHandlerDefinition != null){
			builder.addConstructorArgValue(innerHandlerDefinition);
		} else {
			builder.addConstructorArgReference(ref);
		}
		
		builder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(method, "java.lang.String");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
