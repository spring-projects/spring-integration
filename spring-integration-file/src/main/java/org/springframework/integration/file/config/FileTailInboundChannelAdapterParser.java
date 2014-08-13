/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 *
 */
public class FileTailInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FileTailInboundChannelAdapterFactoryBean.class);

		if (StringUtils.hasText(channelName)) {
			builder.addPropertyReference("outputChannel", channelName);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "native-options");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "file");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-scheduler");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delay");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "file-delay");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "end");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reopen");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");

		return builder.getBeanDefinition();
	}

}
