/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.mail.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;inbound-channel-adapter&gt; element of Spring
 * Integration's 'mail' namespace. 
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class MailInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	private static final String BASE_PACKAGE = "org.springframework.integration.mail";


	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				BASE_PACKAGE + ".MailReceivingMessageSource");
		builder.addConstructorArgReference(this.parseMailReceiver(element, parserContext));
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private String parseMailReceiver(Element element, ParserContext parserContext) {
		String storeUri = element.getAttribute("store-uri");
		Assert.hasText(storeUri, "the 'store-uri' attribute is required");
		BeanDefinitionBuilder receiverBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				BASE_PACKAGE + ".config.MailReceiverFactoryBean");
		receiverBuilder.addPropertyValue("storeUri", storeUri);
		String propertiesRef = element.getAttribute("java-mail-properties");
		if (StringUtils.hasText(propertiesRef)) {
			receiverBuilder.addPropertyReference("javaMailProperties", propertiesRef);
		}
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		if (pollerElement != null) {
			String mmpp = pollerElement.getAttribute("max-messages-per-poll");
			if (StringUtils.hasText(mmpp)) {
				receiverBuilder.addPropertyValue("maxFetchSize", mmpp);
			}
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				receiverBuilder.getBeanDefinition(), parserContext.getRegistry());
	}

}
