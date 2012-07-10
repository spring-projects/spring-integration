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

package org.springframework.integration.config.xml;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;publish-subscribe-channel&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class PublishSubscribeChannelParser extends AbstractChannelParser {

	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				PublishSubscribeChannel.class);
		String taskExecutorRef = element.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutorRef)) {
			builder.addConstructorArgReference(taskExecutorRef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-handler");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-failures");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		this.setMaxSubscribersProperty(parserContext, builder, element,
				IntegrationNamespaceUtils.DEFAULT_MAX_MULTICAST_SUBSCRIBERS_PROPERTY_NAME);
		return builder;
	}

}
