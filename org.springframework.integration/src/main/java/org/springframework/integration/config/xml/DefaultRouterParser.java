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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;router/&gt; element.
 * 
 * @author Mark Fisher
 */
public class DefaultRouterParser extends AbstractDelegatingConsumerEndpointParser {

	@Override
	String getFactoryBeanClassName() {
		return IntegrationNamespaceUtils.BASE_PACKAGE + ".config.RouterFactoryBean";
	}

	@Override
	boolean hasDefaultOption() {
		return false;
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String resolverBeanName = element.getAttribute("channel-resolver");
		if (!StringUtils.hasText(resolverBeanName)) {
			BeanDefinitionBuilder resolverBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".channel.BeanFactoryChannelResolver");
			resolverBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					resolverBuilder.getBeanDefinition(), parserContext.getRegistry());
		}
		builder.addPropertyReference("channelResolver", resolverBeanName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "default-output-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "resolution-required");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-channel-name-resolution-failures");
	}

}
