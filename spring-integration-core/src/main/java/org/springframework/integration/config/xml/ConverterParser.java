/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ConverterParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		BeanComponentDefinition converterDefinition =
				IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		if (converterDefinition != null) {
			registerConverter(registry, converterDefinition);
		}
		else {
			String beanName = element.getAttribute("ref");
			Assert.isTrue(StringUtils.hasText(beanName),
					"Either a 'ref' attribute pointing to a Converter " +
							"or a <bean> sub-element defining a Converter is required.");
			registerConverter(registry, new RuntimeBeanReference(beanName));
		}
		return null;
	}

	private static void registerConverter(BeanDefinitionRegistry registry, BeanMetadataElement targetBeanDefinition) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(
								IntegrationContextUtils.BASE_PACKAGE +
										".config.ConverterRegistrar.IntegrationConverterRegistration")
						.addConstructorArgValue(targetBeanDefinition);
		BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
	}

}
