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

package org.springframework.integration.kafka.config.xml;

import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Soby Chacko
 * @author Ilayaperumal Gopinathan
 * @since 0.5
 */
public class KafkaProducerContextParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(final Element element) {
		return KafkaProducerContext.class;
	}

	@Override
	protected void doParse(final Element element, final ParserContext parserContext, final BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		final Element topics = DomUtils.getChildElementByTagName(element, "producer-configurations");
		parseProducerConfigurations(topics, parserContext, builder, element);
	}

	private void parseProducerConfigurations(Element topics, ParserContext parserContext,
			BeanDefinitionBuilder builder, Element parentElem) {
		Map<String, BeanMetadataElement> producerConfigurationsMap = new ManagedMap<String, BeanMetadataElement>();

		for (Element producerConfiguration : DomUtils.getChildElementsByTagName(topics, "producer-configuration")) {
			BeanDefinitionBuilder producerConfigurationBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ProducerConfiguration.class);

			BeanDefinitionBuilder producerMetadataBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ProducerMetadata.class);
			producerMetadataBuilder.addConstructorArgValue(producerConfiguration.getAttribute("topic"));
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"value-encoder");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"key-encoder");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"key-class-type");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"value-class-type");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"partitioner");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"compression-codec");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"async");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"batch-num-messages");
			AbstractBeanDefinition producerMetadataBeanDefinition = producerMetadataBuilder.getBeanDefinition();

			String producerPropertiesBean = parentElem.getAttribute("producer-properties");

			BeanDefinitionBuilder producerFactoryBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ProducerFactoryBean.class);
			producerFactoryBuilder.addConstructorArgValue(producerMetadataBeanDefinition);

			final String brokerList = producerConfiguration.getAttribute("broker-list");
			if (StringUtils.hasText(brokerList)) {
				producerFactoryBuilder.addConstructorArgValue(producerConfiguration.getAttribute("broker-list"));
			}

			if (StringUtils.hasText(producerPropertiesBean)) {
				producerFactoryBuilder.addConstructorArgReference(producerPropertiesBean);
			}

			AbstractBeanDefinition producerFactoryBeanDefinition = producerFactoryBuilder.getBeanDefinition();

			producerConfigurationBuilder.addConstructorArgValue(producerMetadataBeanDefinition);
			producerConfigurationBuilder.addConstructorArgValue(producerFactoryBeanDefinition);

			AbstractBeanDefinition producerConfigurationBeanDefinition =
					producerConfigurationBuilder.getBeanDefinition();
			producerConfigurationsMap.put(producerConfiguration.getAttribute("topic"),
					producerConfigurationBeanDefinition);
		}

		builder.addPropertyValue("producerConfigurations", producerConfigurationsMap);
	}

}
