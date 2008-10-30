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
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.util.Assert;

/**
 * Parser for the &lt;inbound-channel-adapter&gt; element of Spring
 * Integration's 'mail' namespace. 
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class MailInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		String uri = element.getAttribute("store-uri");
		//String propertiesRef = element.getAttribute("javaMailProperties");
		Assert.hasText(uri, "the 'store-uri' attribute is required");
		boolean isPop3 = uri.toLowerCase().startsWith("pop3");
		boolean isImap = uri.toLowerCase().startsWith("imap");
		Assert.isTrue(isPop3 || isImap, "the 'store-uri' must begin with 'pop3' or 'imap'");
		MailReceiver mailReceiver = isPop3 ? new Pop3MailReceiver(uri) : new ImapMailReceiver(uri);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MailReceivingMessageSource.class);
		/*
		if (StringUtils.hasText(propertiesRef)) {
			folderConnectionBuilder.addPropertyReference("javaMailProperties",
					propertiesRef);
		}
		*/
		builder.addConstructorArgValue(mailReceiver);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
