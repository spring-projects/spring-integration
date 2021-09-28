/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jpa.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.inbound.JpaPollingChannelAdapter;

/**
 * The JPA Inbound Channel adapter parser.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class JpaInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder jpaPollingChannelAdapterBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(JpaPollingChannelAdapter.class);

		BeanDefinitionBuilder jpaExecutorBuilder = JpaParserUtils.getJpaExecutorBuilder(element, parserContext);

		BeanDefinition definition =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("max-results",
						"max-results-expression", parserContext, element, false);
		if (definition != null) {
			jpaExecutorBuilder.addPropertyValue("maxResultsExpression", definition);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "delete-after-poll");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "flush-after-delete", "flush");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "delete-in-batch");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "expect-single-result");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(jpaExecutorBuilder, element, "parameter-source");

		BeanDefinition jpaExecutorBuilderBeanDefinition = jpaExecutorBuilder.getBeanDefinition();
		String channelAdapterId =
				resolveId(element,
						new RootBeanDefinition(SourcePollingChannelAdapterFactoryBean.class),
						parserContext);
		String jpaExecutorBeanName = channelAdapterId + ".jpaExecutor";

		parserContext.registerBeanComponent(new BeanComponentDefinition(jpaExecutorBuilderBeanDefinition,
				jpaExecutorBeanName));

		jpaPollingChannelAdapterBuilder.addConstructorArgReference(jpaExecutorBeanName);

		return jpaPollingChannelAdapterBuilder.getBeanDefinition();
	}

}
