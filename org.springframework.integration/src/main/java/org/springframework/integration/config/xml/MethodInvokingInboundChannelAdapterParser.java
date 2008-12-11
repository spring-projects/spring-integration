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
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;inbound-channel-adapter/&gt; element.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		String sourceRef = element.getAttribute("ref");
		if (!StringUtils.hasText(sourceRef)) {
			parserContext.getReaderContext().error("The 'ref' attribute is required.", element);
		}
		String methodName = element.getAttribute("method");
		if (StringUtils.hasText(methodName)) {
			BeanDefinitionBuilder invokerBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".message.MethodInvokingMessageSource");
			invokerBuilder.addPropertyReference("object", sourceRef);
			invokerBuilder.addPropertyValue("methodName", methodName);
			sourceRef = BeanDefinitionReaderUtils.registerWithGeneratedName(
					invokerBuilder.getBeanDefinition(), parserContext.getRegistry());
		}
		return sourceRef;
	}

}
