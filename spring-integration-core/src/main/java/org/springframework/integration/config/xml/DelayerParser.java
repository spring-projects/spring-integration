/*
 * Copyright 2002-2010 the original author or authors.
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
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;delayer&gt; element.
 * 
 * @author Mark Fisher
 * @since 1.0.3
 */
public class DelayerParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".handler.DelayHandler");
		String defaultDelay = element.getAttribute("default-delay");
		if (!StringUtils.hasText(defaultDelay)) {
			parserContext.getReaderContext().error("The 'default-delay' attribute is required.", element);
			return null;
		}
		builder.addConstructorArgValue(defaultDelay);
		String scheduler = element.getAttribute("scheduler");
		if (StringUtils.hasText(scheduler)) {
			builder.addConstructorArgReference(scheduler);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-store");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delay-header-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "wait-for-tasks-to-complete-on-shutdown");
		return builder;
	}

}
