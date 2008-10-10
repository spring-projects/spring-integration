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
import org.springframework.integration.mail.DefaultFolderConnection;
import org.springframework.integration.mail.PollingMailSource;
import org.springframework.integration.mail.monitor.MonitoringStrategy;
import org.springframework.integration.mail.monitor.PollingMonitoringStrategy;
import org.springframework.integration.mail.monitor.Pop3PollingMonitoringStrategy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Jonas Partner
 */
public class PollingMailSourceParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return PollingMailSource.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String mailConvertorRef = element.getAttribute("convertor");
		String uri = element.getAttribute("store-uri");
		String propertiesRef = element.getAttribute("javaMailProperties");
		Assert.hasText(uri, "the 'store-uri' attribute is required");
		BeanDefinitionBuilder folderConnectionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(DefaultFolderConnection.class);
		MonitoringStrategy monitoringStrategy = null;
		if (uri.toLowerCase().startsWith("pop3")) {
			monitoringStrategy = new Pop3PollingMonitoringStrategy();
		}
		else if (uri.toLowerCase().startsWith("imap")) {
			monitoringStrategy = new PollingMonitoringStrategy();
		}
		else {
			throw new IllegalArgumentException(
					"unable to determine monitoring strategy for store-uri [" + uri + "]");
		}
		folderConnectionBuilder.addConstructorArgValue(uri);
		folderConnectionBuilder.addConstructorArgValue(monitoringStrategy);
		// set polling true
		folderConnectionBuilder.addConstructorArgValue(true);

		if (StringUtils.hasText(propertiesRef)) {
			folderConnectionBuilder.addPropertyReference("javaMailProperties",
					propertiesRef);
		}
		String folderConnectionName = parserContext.getReaderContext()
				.registerWithGeneratedName(folderConnectionBuilder.getBeanDefinition());
		builder.addDependsOn(folderConnectionName);
		builder.addConstructorArgValue(folderConnectionBuilder.getBeanDefinition());
		if (StringUtils.hasText(mailConvertorRef)) {
			builder.addPropertyReference("convertor", mailConvertorRef);
		}
	}

}
