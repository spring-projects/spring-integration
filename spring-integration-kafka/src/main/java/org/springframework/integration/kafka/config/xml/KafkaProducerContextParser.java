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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.integration.kafka.util.EncoderAdaptingSerializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Soby Chacko
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @since 0.5
 */
public class KafkaProducerContextParser extends AbstractSimpleBeanDefinitionParser {

	private static final Log log = LogFactory.getLog(KafkaProducerContextParser.class);

	@Override
	protected Class<?> getBeanClass(final Element element) {
		return KafkaProducerContext.class;
	}

	@Override
	protected void doParse(final Element element, final ParserContext parserContext, final BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");

		final Element topics = DomUtils.getChildElementByTagName(element, "producer-configurations");
		parseProducerConfigurations(topics, parserContext, builder, element);
	}


	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !"producer-properties".equals(attributeName) && super.isEligibleAttribute(attributeName);
	}

	private void parseProducerConfigurations(Element topics, ParserContext parserContext,
			BeanDefinitionBuilder builder, Element parentElem) {
		Map<String, BeanMetadataElement> producerConfigurationsMap = new ManagedMap<String, BeanMetadataElement>();

		for (Element producerConfiguration : DomUtils.getChildElementsByTagName(topics, "producer-configuration")) {

			BeanDefinitionBuilder producerMetadataBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ProducerMetadata.class);
			producerMetadataBuilder.addConstructorArgValue(producerConfiguration.getAttribute("topic"));
			producerMetadataBuilder.addConstructorArgValue(producerConfiguration.getAttribute("key-class-type"));
			producerMetadataBuilder.addConstructorArgValue(producerConfiguration.getAttribute("value-class-type"));

			String keySerializer = producerConfiguration.getAttribute("key-serializer");
			String keyEncoder = producerConfiguration.getAttribute("key-encoder");
			Assert.isTrue((StringUtils.hasText(keySerializer) ^ StringUtils.hasText(keyEncoder)),
					"Exactly one of 'key-serializer' or 'key-encoder' must be specified");
			if (StringUtils.hasText(keyEncoder)) {
				if (log.isWarnEnabled()) {
					log.warn("'key-encoder' is a deprecated option, use 'key-serializer' instead.");
				}
				BeanDefinitionBuilder encoderAdaptingSerializerBean = BeanDefinitionBuilder.genericBeanDefinition(EncoderAdaptingSerializer.class);
				encoderAdaptingSerializerBean.addConstructorArgReference(keyEncoder);
				producerMetadataBuilder.addConstructorArgValue(encoderAdaptingSerializerBean.getBeanDefinition());
			}
			else {
				producerMetadataBuilder.addConstructorArgReference(keySerializer);
			}

			String valueSerializer = producerConfiguration.getAttribute("value-serializer");
			String valueEncoder = producerConfiguration.getAttribute("value-encoder");
			Assert.isTrue((StringUtils.hasText(valueSerializer) ^ StringUtils.hasText(valueEncoder)),
					"Exactly one of 'value-serializer' or 'value-encoder' must be specified");
			if (StringUtils.hasText(valueEncoder)) {
				if (log.isWarnEnabled()) {
					log.warn("'value-encoder' is a deprecated option, use 'value-serializer' instead.");
				}
				BeanDefinitionBuilder encoderAdaptingSerializerBean =
						BeanDefinitionBuilder.genericBeanDefinition(EncoderAdaptingSerializer.class);
				encoderAdaptingSerializerBean.addConstructorArgReference(valueEncoder);
				producerMetadataBuilder.addConstructorArgValue(encoderAdaptingSerializerBean.getBeanDefinition());
			}
			else {
				producerMetadataBuilder.addConstructorArgReference(valueSerializer);
			}

			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"partitioner");
			if (StringUtils.hasText(producerConfiguration.getAttribute("partitioner"))) {
				if (log.isWarnEnabled()) {
					log.warn("'partitioner' is a deprecated option. Use the 'kafka_partitionId' message header or " +
							"the partition argument in the send() or convertAndSend() methods");
				}
			}
			IntegrationNamespaceUtils.setValueIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"compression-type");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(producerMetadataBuilder, producerConfiguration,
					"batch-bytes");
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

			BeanDefinitionBuilder producerConfigurationBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ProducerConfiguration.class)
							.addConstructorArgValue(producerMetadataBeanDefinition)
							.addConstructorArgValue(producerFactoryBeanDefinition);
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(producerConfigurationBuilder, producerConfiguration,
					"conversion-service");
			producerConfigurationsMap.put(producerConfiguration.getAttribute("topic"),
					producerConfigurationBuilder.getBeanDefinition());
		}

		builder.addPropertyValue("producerConfigurations", producerConfigurationsMap);
	}

}
