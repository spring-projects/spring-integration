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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.router.BeanNameChannelMapping;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;router/&gt; element.
 * 
 * @author Mark Fisher
 */
public class RouterParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseConsumer(Element element, ParserContext parserContext) {
		String ref = element.getAttribute(REF_ATTRIBUTE);
		Assert.hasText(ref, "The '" + REF_ATTRIBUTE + "' attribute is required.");
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingRouter.class);
		builder.addConstructorArgReference(ref);
		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			builder.addConstructorArgValue(method);
		}
		BeanDefinitionBuilder channelMappingBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(BeanNameChannelMapping.class);
		String channelMappingBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				channelMappingBuilder.getBeanDefinition(), parserContext.getRegistry());
		builder.addPropertyReference("channelMapping", channelMappingBeanName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "default-output-channel");
		return builder;
	}

}
