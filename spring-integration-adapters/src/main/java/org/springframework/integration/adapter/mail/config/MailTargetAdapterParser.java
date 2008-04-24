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

package org.springframework.integration.adapter.mail.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.mail.MailTarget;
import org.springframework.integration.endpoint.TargetEndpoint;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;mail-target/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class MailTargetAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return TargetEndpoint.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		RootBeanDefinition adapterDef = new RootBeanDefinition(MailTarget.class);
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
			adapterDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(mailSenderRef));
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
			adapterDef.getConstructorArgumentValues().addGenericArgumentValue(mailSender);
		}
		else {
			throw new ConfigurationException("Either a 'mail-sender' reference or 'host' property is required.");
		}
		if (StringUtils.hasText(headerGeneratorRef)) {
			adapterDef.getPropertyValues().addPropertyValue(
					"headerGenerator", new RuntimeBeanReference(headerGeneratorRef));
		}
		String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, adapterBeanName));
		builder.addConstructorArgReference(adapterBeanName);
		String channel = element.getAttribute("channel");
		Subscription subscription = new Subscription(channel);
		builder.addPropertyValue("subscription", subscription);
	}

}
