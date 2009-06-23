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

package org.springframework.integration.config.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;transformer/&gt; element.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class TransformerParser extends AbstractInnerDefinitionAwareEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseEndpoint(Element element, ParserContext parserContext, BeanDefinition innerDefinition) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".config.TransformerFactoryBean");
	
		if (innerDefinition != null){
			builder.addPropertyValue("targetObject", innerDefinition);
		} else {
			String ref = element.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(ref)) {
				parserContext.getReaderContext().error("Either \"ref\" attribute or inner bean (<bean/>) definition of concrete implementation of " +
														"this Transformer is required.", element);
				return null;
			}
			builder.addPropertyReference("targetObject", ref);
		}	
		
		String method = element.getAttribute(METHOD_ATTRIBUTE);
		if (StringUtils.hasText(method)) {
			builder.addPropertyValue("targetMethodName", method);
		}
		return builder;
	}
}
