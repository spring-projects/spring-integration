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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.support.ConsumerConfigFactoryBean;
import org.springframework.integration.kafka.support.ConsumerConfiguration;
import org.springframework.integration.kafka.support.ConsumerConnectionProvider;
import org.springframework.integration.kafka.support.ConsumerMetadata;
import org.springframework.integration.kafka.support.KafkaConsumerContext;
import org.springframework.integration.kafka.support.MessageLeftOverTracker;
import org.springframework.integration.kafka.support.TopicFilterConfiguration;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @author Artem Bilan
 * @author Ilayaperumal Gopinathan
 * @since 0.5
 */
public class KafkaConsumerContextParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(final Element element) {
		return KafkaConsumerContext.class;
	}

	@Override
	protected void doParse(final Element element, final ParserContext parserContext, final BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		final Element consumerConfigurations = DomUtils.getChildElementByTagName(element, "consumer-configurations");
		parseConsumerConfigurations(consumerConfigurations, parserContext, builder, element);
	}

	private void parseConsumerConfigurations(final Element consumerConfigurations, final ParserContext parserContext,
			final BeanDefinitionBuilder builder, final Element parentElem) {
		Map<String, BeanMetadataElement> consumerConfigurationsMap = new ManagedMap<String, BeanMetadataElement>();
		for (final Element consumerConfiguration : DomUtils.getChildElementsByTagName(consumerConfigurations, "consumer-configuration")) {
			final BeanDefinitionBuilder consumerConfigurationBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ConsumerConfiguration.class);
			final BeanDefinitionBuilder consumerMetadataBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ConsumerMetadata.class);

			IntegrationNamespaceUtils.setValueIfAttributeDefined(consumerMetadataBuilder, consumerConfiguration,
					"group-id");

			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(consumerMetadataBuilder, consumerConfiguration,
					"value-decoder");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(consumerMetadataBuilder, consumerConfiguration,
					"key-decoder");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(consumerMetadataBuilder, consumerConfiguration,
					"key-class-type");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(consumerMetadataBuilder, consumerConfiguration,
					"value-class-type");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(consumerConfigurationBuilder, consumerConfiguration,
					"max-messages");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(consumerMetadataBuilder, parentElem,
					"consumer-timeout");

			final Map<String, String> topicStreamsMap = new HashMap<String, String>();

			final List<Element> topicConfigurations = DomUtils.getChildElementsByTagName(consumerConfiguration, "topic");

			if (topicConfigurations != null) {
				for (final Element topicConfiguration : topicConfigurations) {
					final String topic = topicConfiguration.getAttribute("id");
					final String streams = topicConfiguration.getAttribute("streams");
					topicStreamsMap.put(topic, streams);
				}
				consumerMetadataBuilder.addPropertyValue("topicStreamMap", topicStreamsMap);
			}

			final Element topicFilter = DomUtils.getChildElementByTagName(consumerConfiguration, "topic-filter");

			if (topicFilter != null) {
				BeanDefinition topicFilterConfigurationBeanDefinition =
						BeanDefinitionBuilder.genericBeanDefinition(TopicFilterConfiguration.class)
								.addConstructorArgValue(topicFilter.getAttribute("pattern"))
								.addConstructorArgValue(topicFilter.getAttribute("streams"))
								.addConstructorArgValue(topicFilter.getAttribute("exclude"))
								.getBeanDefinition();
				consumerMetadataBuilder.addPropertyValue("topicFilterConfiguration",
						topicFilterConfigurationBeanDefinition);
			}

			final AbstractBeanDefinition consumerMetadataBeanDefintiion = consumerMetadataBuilder.getBeanDefinition();

			final String zookeeperConnectBean = parentElem.getAttribute("zookeeper-connect");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, parentElem, zookeeperConnectBean);

			final String consumerPropertiesBean = parentElem.getAttribute("consumer-properties");

			final BeanDefinitionBuilder consumerConfigFactoryBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ConsumerConfigFactoryBean.class);
			consumerConfigFactoryBuilder.addConstructorArgValue(consumerMetadataBeanDefintiion);

			if (StringUtils.hasText(zookeeperConnectBean)) {
				consumerConfigFactoryBuilder.addConstructorArgReference(zookeeperConnectBean);
			}

			if (StringUtils.hasText(consumerPropertiesBean)) {
				consumerConfigFactoryBuilder.addConstructorArgReference(consumerPropertiesBean);
			}

			AbstractBeanDefinition consumerConfigFactoryBuilderBeanDefinition =
					consumerConfigFactoryBuilder.getBeanDefinition();

			BeanDefinitionBuilder consumerConnectionProviderBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ConsumerConnectionProvider.class);
			consumerConnectionProviderBuilder.addConstructorArgValue(consumerConfigFactoryBuilderBeanDefinition);

			AbstractBeanDefinition consumerConnectionProviderBuilderBeanDefinition =
					consumerConnectionProviderBuilder.getBeanDefinition();

			BeanDefinitionBuilder messageLeftOverBeanDefinitionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(MessageLeftOverTracker.class);
			AbstractBeanDefinition messageLeftOverBeanDefinition =
					messageLeftOverBeanDefinitionBuilder.getBeanDefinition();

			consumerConfigurationBuilder.addConstructorArgValue(consumerMetadataBeanDefintiion);
			consumerConfigurationBuilder.addConstructorArgValue(consumerConnectionProviderBuilderBeanDefinition);
			consumerConfigurationBuilder.addConstructorArgValue(messageLeftOverBeanDefinition);

			AbstractBeanDefinition consumerConfigurationBeanDefinition =
					consumerConfigurationBuilder.getBeanDefinition();
			consumerConfigurationsMap.put(consumerConfiguration.getAttribute("group-id"),
					consumerConfigurationBeanDefinition);
		}
		builder.addPropertyValue("consumerConfigurations", consumerConfigurationsMap);
	}

}
