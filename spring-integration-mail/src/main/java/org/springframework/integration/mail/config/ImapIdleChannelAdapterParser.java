/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;imap-idle-channel-adapter&gt; element in the 'mail' namespace.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class ImapIdleChannelAdapterParser extends AbstractSingleBeanDefinitionParser {

	private static final String BASE_PACKAGE = "org.springframework.integration.mail";


	protected String getBeanClassName(Element element) {
		return BASE_PACKAGE + ".ImapIdleChannelAdapter";
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String channel = element.getAttribute("channel");
		Assert.hasText(channel, "the 'channel' attribute is required");
		builder.addConstructorArgValue(this.parseImapMailReceiver(element, parserContext));
		builder.addPropertyReference("outputChannel", channel);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel", "errorChannel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
	}

	private BeanDefinition parseImapMailReceiver(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder receiverBuilder = BeanDefinitionBuilder.genericBeanDefinition(BASE_PACKAGE + ".ImapMailReceiver");
		Object source = parserContext.extractSource(element);
		String uri = element.getAttribute("store-uri");
		if (StringUtils.hasText(uri)) {
			receiverBuilder.addConstructorArgValue(uri);
		}
		String session = element.getAttribute("session");
		if (StringUtils.hasText(session)) {
			if (element.hasAttribute("java-mail-properties") || element.hasAttribute("authenticator")) {
				parserContext.getReaderContext().error("Neither 'java-mail-properties' nor 'authenticator' " +
						"references are allowed when a 'session' reference has been provided.", source);
			}
			receiverBuilder.addPropertyReference("session", session);
		}
		else {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(receiverBuilder, element, "java-mail-properties");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(receiverBuilder, element, "authenticator", "javaMailAuthenticator");
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "max-fetch-size");
		receiverBuilder.addPropertyValue("shouldDeleteMessages", element.getAttribute("should-delete-messages"));
		String markAsRead = element.getAttribute("should-mark-messages-as-read");
		if (StringUtils.hasText(markAsRead)){
			receiverBuilder.addPropertyValue("shouldMarkMessagesAsRead", markAsRead);
		}
		return receiverBuilder.getBeanDefinition(); 
	}
}
