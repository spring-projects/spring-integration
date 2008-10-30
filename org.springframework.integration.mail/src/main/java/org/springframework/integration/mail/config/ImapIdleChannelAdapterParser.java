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

package org.springframework.integration.mail.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;imap-idle-channel-adapter&gt; element in the 'mail' namespace.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class ImapIdleChannelAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return ImapIdleChannelAdapter.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String channel = element.getAttribute("channel");
		String uri = element.getAttribute("store-uri");
		String taskExecutorRef = element.getAttribute("task-executor");
		String propertiesRef = element.getAttribute("java-mail-properties");
		Assert.hasText(channel, "the 'channel' attribute is required");
		Assert.hasText(uri, "the 'store-uri' attribute is required");
		Assert.isTrue(uri.toLowerCase().startsWith("imap"),
				"store-uri must start with 'imap' for the imap idle channel adapter");
		builder.addConstructorArgValue(uri);
		if (StringUtils.hasText(propertiesRef)) {
			builder.addPropertyReference("javaMailProperties", propertiesRef);
		}
		if (StringUtils.hasLength(taskExecutorRef)) {
			builder.addPropertyReference("taskExecutor", taskExecutorRef);
		}
		builder.addPropertyValue("shouldDeleteMessages",
				!"false".equals(element.getAttribute("should-delete-messages")));
		builder.addPropertyReference("outputChannel", channel);
	}

}
