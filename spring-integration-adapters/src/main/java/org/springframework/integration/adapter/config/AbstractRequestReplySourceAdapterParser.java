/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.util.StringUtils;

/**
 * Base class for request-reply source adapter parsers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractRequestReplySourceAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected abstract Class<?> getBeanClass(Element element);

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(id)) {
			id = element.getAttribute("name");
		}
		if (!StringUtils.hasText(id)) {
			id = parserContext.getReaderContext().generateBeanName(definition);
		}
		return id;
	}

	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String channelRef = element.getAttribute("channel");
		if (!StringUtils.hasText(channelRef)) {
			throw new MessagingConfigurationException("a 'channel' reference is required");
		}
		builder.addPropertyReference("channel", channelRef);
		builder.addPropertyValue("expectReply", element.getAttribute("expect-reply").equals("true"));
		String sendTimeout = element.getAttribute("send-timeout");
		if (StringUtils.hasText(sendTimeout)) {
			builder.addPropertyValue("sendTimeout", Long.parseLong(sendTimeout));
		}
		String receiveTimeout = element.getAttribute("receive-timeout");
		if (StringUtils.hasText(receiveTimeout)) {
			builder.addPropertyValue("receiveTimeout", Long.parseLong(receiveTimeout));
		}
	}

}
