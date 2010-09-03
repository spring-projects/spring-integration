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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MBeanExporterParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		Object mbeanServer = getMBeanServer(element, parserContext);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.integration.monitor.IntegrationMBeanExporter");
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "domain");
		builder.addPropertyValue("server", mbeanServer);
		if (StringUtils.hasText(element.getAttribute("operation-channel"))) {
			AbstractBeanDefinition controlBus = getControlBus(element, parserContext, mbeanServer, builder
					.getRawBeanDefinition());
			parserContext.getRegistry().registerBeanDefinition(
					parserContext.getReaderContext().generateBeanName(controlBus), controlBus);
		}
		return builder.getBeanDefinition();
	}

	private Object getMBeanServer(Element element, ParserContext parserContext) {
		String mbeanServer = element.getAttribute("mbean-server");
		if (StringUtils.hasText(mbeanServer)) {
			return new RuntimeBeanReference(mbeanServer);
		}
		else {
			return MBeanServerFactory.createMBeanServer();
		}
	}

	private AbstractBeanDefinition getControlBus(Element element, ParserContext parserContext, Object mbeanServer,
			Object mbeanExporter) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.integration.control.ControlBus");
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		builder.addConstructorArgValue(mbeanExporter);
		builder.addConstructorArgValue(mbeanServer);
		builder.addConstructorArgReference(element.getAttribute("operation-channel"));
		return builder.getBeanDefinition();
	}

}
