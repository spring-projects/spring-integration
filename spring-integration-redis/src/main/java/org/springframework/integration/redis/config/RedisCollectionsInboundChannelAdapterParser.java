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
package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.inbound.RedisListInboundChannelAdapter;
import org.springframework.integration.redis.inbound.RedisZsetInboundChannelAdapter;
/**
 * Parser for Redis zset/list-inbound-channel-adapter
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisCollectionsInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = this.createBuilder(element, parserContext);
		builder.addConstructorArgReference(element.getAttribute("connection-factory"));
		RootBeanDefinition expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
		expressionDef.getConstructorArgumentValues().addGenericArgumentValue(element.getAttribute("key-expression"));
		builder.addConstructorArgValue(expressionDef);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "score-range");

		String beanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
		return new RuntimeBeanReference(beanName);
	}

	private BeanDefinitionBuilder createBuilder(Element element, ParserContext parserContext){
		BeanDefinitionBuilder builder = null;
		if (element.getLocalName().startsWith("list")){
			builder =  BeanDefinitionBuilder.genericBeanDefinition(RedisListInboundChannelAdapter.class);
		}
		else if (element.getLocalName().startsWith("zset")){
			builder = BeanDefinitionBuilder.genericBeanDefinition(RedisZsetInboundChannelAdapter.class);
		}
		else {
			parserContext.getReaderContext().error("Unrecognized element", element.getLocalName());
		}

		return builder;
	}

}
