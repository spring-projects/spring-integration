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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class ConverterParser extends AbstractBeanDefinitionParser {

	private final ManagedSet<Object> converters = new ManagedSet<Object>();

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		this.initializeConversionServiceInfrastructureIfNecessary(parserContext);
		BeanComponentDefinition converterDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		if (converterDefinition != null) {
			this.converters.add(converterDefinition);			
		}
		else {
			String beanName = element.getAttribute("ref");
			Assert.isTrue(StringUtils.hasText(beanName),
					"Either a 'ref' attribute pointing to a Converter or a <bean> sub-element defining a Converter is required.");
			this.converters.add(new RuntimeBeanReference(beanName));
		}
		return null;
	}

	private void initializeConversionServiceInfrastructureIfNecessary(ParserContext parserContext) {
		synchronized (this.initializationMonitor) {
			if (!this.initialized) {
				String contextPackage = "org.springframework.integration.context.";
				BeanDefinitionBuilder creatorBuilder = BeanDefinitionBuilder.rootBeanDefinition(contextPackage + "ConversionServiceCreator");
				BeanDefinitionReaderUtils.registerWithGeneratedName(creatorBuilder.getBeanDefinition(), parserContext.getRegistry());
				BeanDefinitionBuilder conversionServiceBuilder = BeanDefinitionBuilder.rootBeanDefinition(contextPackage + "ConverterRegistrar");
				conversionServiceBuilder.addConstructorArgValue(converters);
				BeanDefinitionReaderUtils.registerWithGeneratedName(conversionServiceBuilder.getBeanDefinition(), parserContext.getRegistry());
				this.initialized = true;
			}
		}
	}

}
