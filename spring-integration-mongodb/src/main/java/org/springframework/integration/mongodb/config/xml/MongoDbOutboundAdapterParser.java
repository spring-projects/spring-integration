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
package org.springframework.integration.mongodb.config.xml;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mongodb.outbound.MongoDbMessageHandler;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * The parser for the outbound-channel-adapter element in mongodb namespace
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbOutboundAdapterParser extends
		AbstractOutboundChannelAdapterParser {

	/* (non-Javadoc)
	 * @see org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser#parseConsumer(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
	 */
	@Override
	protected AbstractBeanDefinition parseConsumer(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder  builder = BeanDefinitionBuilder.genericBeanDefinition(MongoDbMessageHandler.class);
		String beanRef = element.getAttribute("mongo-db-factory");
		builder.addConstructorArgReference(beanRef);
		String collection = element.getAttribute("collection");
		if(StringUtils.hasText(collection)) {
			builder.addConstructorArgValue(collection);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "write-result-checking");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "write-concern");
		return builder.getBeanDefinition();
	}
}
