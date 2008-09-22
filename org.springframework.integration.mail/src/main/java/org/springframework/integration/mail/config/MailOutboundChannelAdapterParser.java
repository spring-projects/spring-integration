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
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.config.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.mail.MailSendingMessageConsumer;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element of the 'mail' namespace. 
 * 
 * @author Mark Fisher
 */
public class MailOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	protected Class<?> getBeanClass(Element element) {
		return MailSendingMessageConsumer.class;
	}

	@Override
	protected String parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MailSendingMessageConsumer.class);
		String mailSenderRef = element.getAttribute("mail-sender");
		String host = element.getAttribute("host");
		String username = element.getAttribute("username");
		String password = element.getAttribute("password");
		String headerGeneratorRef = element.getAttribute("header-generator");
		if (StringUtils.hasText(mailSenderRef)) {
			if (StringUtils.hasText(host) || StringUtils.hasText(username) || StringUtils.hasText(password)) {
				throw new ConfigurationException("The 'host', 'username', and 'password' properties " +
						"should not be provided when using a 'mail-sender' reference.");
			}
			builder.addConstructorArgReference(mailSenderRef);
		}
		else if (StringUtils.hasText(host)) {
			JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
			mailSender.setHost(host);
			if (StringUtils.hasText(username)) {
				mailSender.setUsername(username);
			}
			if (StringUtils.hasText(password)) {
				mailSender.setPassword(password);
			}
			builder.addConstructorArgValue(mailSender);
		}
		else {
			throw new ConfigurationException("Either a 'mail-sender' reference or 'host' property is required.");
		}
		if (StringUtils.hasText(headerGeneratorRef)) {
			builder.addPropertyReference("headerGenerator", headerGeneratorRef);
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
