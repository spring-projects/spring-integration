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
package org.springframework.integration.jpa.config.xml;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.inbound.JpaPollingChannelAdapter;
import org.w3c.dom.Element;

/**
 * The JPA Inbound Channel adapter parser
 *
 * @author Amol Nayak
 * @since 2.2
 *
 *
 */
public class JpaInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser{


	protected BeanMetadataElement parseSource(Element element,
			ParserContext parserContext) {

		final BeanDefinitionBuilder jpaExecutorBuilder = JpaParserUtils.getJpaExecutorBuilder(element, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "delete-after-poll");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "delete-per-row");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "expect-single-result");

		final BeanDefinition jpaExecutorBuilderBeanDefinition = jpaExecutorBuilder.getBeanDefinition();
		final String jpaExecutorBeanName = BeanDefinitionReaderUtils.generateBeanName(jpaExecutorBuilderBeanDefinition, parserContext.getRegistry());

		parserContext.registerBeanComponent(new BeanComponentDefinition(jpaExecutorBuilderBeanDefinition, jpaExecutorBeanName));

		final BeanDefinitionBuilder jpaPollingChannelAdapterBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(JpaPollingChannelAdapter.class);

		jpaPollingChannelAdapterBuilder.addConstructorArgReference(jpaExecutorBeanName);

		return jpaPollingChannelAdapterBuilder.getBeanDefinition();
	}
}
