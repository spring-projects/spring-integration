/*
 * Copyright 2002-2014 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.twitter.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.twitter.outbound.DirectMessageSendingMessageHandler;
import org.springframework.integration.twitter.outbound.StatusUpdatingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for all outbound Twitter adapters
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class TwitterOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		Class<?> clazz = determineClass(element, parserContext);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(clazz);
		builder.addConstructorArgReference(element.getAttribute("twitter-template"));
		String tweetDataExpression = element.getAttribute("tweet-data-expression");
		if (StringUtils.hasText(tweetDataExpression)) {
			builder.addPropertyValue("tweetDataExpression",
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
							.addConstructorArgValue(tweetDataExpression)
							.getBeanDefinition());
		}
		return builder.getBeanDefinition();
	}


	private static Class<?> determineClass(Element element, ParserContext parserContext) {
		Class<?> clazz = null;
		String elementName = element.getLocalName().trim();
		if ("outbound-channel-adapter".equals(elementName)) {
			clazz = StatusUpdatingMessageHandler.class;
		}
		else if ("dm-outbound-channel-adapter".equals(elementName)) {
			clazz = DirectMessageSendingMessageHandler.class;
		}
		else {
			parserContext.getReaderContext().error("element '" + elementName + "' is not supported by this parser.", element);
		}
		return clazz;
	}

}
