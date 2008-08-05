/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.mail.DefaultFolderConnection;
import org.springframework.integration.adapter.mail.SubscribableMailSource;
import org.springframework.integration.adapter.mail.monitor.ImapIdleMonitoringStrategy;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * 
 * @author Jonas Partner
 *
 */
public class SubscribableImapIdleMailSourceParser extends
		AbstractSingleBeanDefinitionParser {
	protected Class<?> getBeanClass(Element element) {
		return SubscribableMailSource.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		String mailConvertorRef = element.getAttribute("convertor");
		String uri = element.getAttribute("store-uri");
		String taskExecutorRef = element.getAttribute("task-executor");
		String propertiesRef = element.getAttribute("javaMailProperties");
		if (!StringUtils.hasLength(uri)) {
			throw new ConfigurationException(
					"A value for the store-uri attribue is required");
		}
		if (!StringUtils.hasLength(taskExecutorRef)) {
			throw new ConfigurationException(
					"A value for the task-executor attribute is required");
		}

		BeanDefinitionBuilder folderConnectionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(DefaultFolderConnection.class);
		String storeType = uri.substring(0, 4).toLowerCase();

		if (!storeType.equals("imap")) {
			throw new ConfigurationException(
					"store-uri must start with imap for the imap idle source");
		}
		folderConnectionBuilder.addConstructorArgValue(uri);
		folderConnectionBuilder
				.addConstructorArgValue(new ImapIdleMonitoringStrategy());
		// set polling false
		folderConnectionBuilder.addConstructorArgValue(false);
		if (StringUtils.hasText(propertiesRef)) {
			folderConnectionBuilder.addPropertyReference("javaMailProperties",
					propertiesRef);
		}

		builder.addConstructorArgValue(folderConnectionBuilder
				.getBeanDefinition());
		builder.addConstructorArgReference(taskExecutorRef);

		if (StringUtils.hasText(mailConvertorRef)) {
			builder.addPropertyReference("convertor", mailConvertorRef);
		}
	}
}
