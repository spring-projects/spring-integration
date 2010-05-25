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

package org.springframework.integration.jmx.config;

import javax.management.MBeanServerFactory;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class ControlBusParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.control.ControlBus";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		Object source = parserContext.extractSource(element);
		builder.getRawBeanDefinition().setSource(source);
		String mbeanServer = element.getAttribute("mbean-server");
		if (StringUtils.hasText(mbeanServer)) {
			builder.addConstructorArgReference(mbeanServer);
		}
		else {
			builder.addConstructorArgValue(MBeanServerFactory.createMBeanServer());
		}
		String domain = element.getAttribute("domain");
		if (StringUtils.hasText(domain)) {
			builder.addConstructorArgValue(domain);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "operation-channel");
	}

}
