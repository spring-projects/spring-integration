/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mail.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element of the 'mail' namespace.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MailOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MailSendingMessageHandler.class);
		String mailSenderRef = element.getAttribute("mail-sender");
		String host = element.getAttribute("host");
		boolean hasHost = StringUtils.hasText(host);
		String port = element.getAttribute("port");
		String username = element.getAttribute("username");
		String password = element.getAttribute("password");
		String javaMailProperties = element.getAttribute("java-mail-properties");
		boolean hasJavaMailProperties = StringUtils.hasText(javaMailProperties);
		if (StringUtils.hasText(mailSenderRef)) {
			Assert.isTrue(!hasHost
							&& !StringUtils.hasText(username)
							&& !StringUtils.hasText(password),
					"The 'host', 'username', and 'password' properties " +
							"should not be provided when using a 'mail-sender' reference.");
			builder.addConstructorArgReference(mailSenderRef);
		}
		else {
			Assert.isTrue(!hasHost || !hasJavaMailProperties,
					"Either a 'mail-sender' or 'java-mail-properties' reference or 'host' property is required.");
			BeanDefinitionBuilder mailSenderBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(JavaMailSenderImpl.class);
			if (hasHost) {
				mailSenderBuilder.addPropertyValue("host", host);
			}
			if (StringUtils.hasText(username)) {
				mailSenderBuilder.addPropertyValue("username", username);
			}
			if (StringUtils.hasText(password)) {
				mailSenderBuilder.addPropertyValue("password", password);
			}
			if (StringUtils.hasText(port)) {
				mailSenderBuilder.addPropertyValue("port", port);
			}
			if (hasJavaMailProperties) {
				mailSenderBuilder.addPropertyReference("javaMailProperties", javaMailProperties);
			}

			String mailSenderBeanName =
					BeanDefinitionReaderUtils.registerWithGeneratedName(mailSenderBuilder.getBeanDefinition(),
							parserContext.getRegistry());
			builder.addConstructorArgReference(mailSenderBeanName);
		}
		return builder.getBeanDefinition();
	}

}
