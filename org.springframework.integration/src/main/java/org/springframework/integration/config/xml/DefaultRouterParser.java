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
 * Parser for the &lt;router/&gt; element.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class DefaultRouterParser extends AbstractRouterParser {

	@Override
	protected void parseRouter(Element element, BeanDefinitionBuilder builder, ParserContext parserContext) {
		BeanDefinition innerDefinition 	= this.parseInnerHandlerDefinition(element, parserContext);
		if (innerDefinition != null){
			builder.addPropertyValue("targetObject", innerDefinition);
		} else {
			String ref = element.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(ref)) {
				parserContext.getReaderContext().error("The '" + REF_ATTRIBUTE + "' attribute is required.", element);
			}
			builder.addPropertyReference("targetObject", ref);
		}
		
		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			builder.addPropertyValue("targetMethodName", method);
		}
		String resolverBeanName = element.getAttribute("channel-resolver");
		if (!StringUtils.hasText(resolverBeanName)) {
			BeanDefinitionBuilder resolverBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".channel.BeanFactoryChannelResolver");
			resolverBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					resolverBuilder.getBeanDefinition(), parserContext.getRegistry());
		}
		builder.addPropertyReference("channelResolver", resolverBeanName);
	}

}
