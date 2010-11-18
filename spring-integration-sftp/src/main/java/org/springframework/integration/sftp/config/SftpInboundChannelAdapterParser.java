/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.sftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
/**
 * Parser for 'sftp:inbound-channel-adapter'
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
public class SftpInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		String sessionFactoryName = element.getAttribute("session-factory");
		String autoStartup = element.getAttribute("auto-startup");
		
		
		String fileNamePattern = element.getAttribute("filename-pattern");
		String filter = element.getAttribute("filter");
		boolean hasFileNamePattern = StringUtils.hasText(fileNamePattern);
		boolean hasFilter = StringUtils.hasText(filter);
		if (!(hasFileNamePattern ^ hasFilter)) {
			throw new BeanDefinitionStoreException("exactly one of 'filename-pattern' or 'filter' " +
					"is allowed on SFTP inbound adapter");
		}
		
		BeanDefinitionBuilder sessionPollBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.sftp.session.QueuedSftpSessionPool");
		sessionPollBuilder.addConstructorArgReference(sessionFactoryName);
		sessionPollBuilder.addPropertyValue("autoStartup", autoStartup);
		String sessionPollName = 
			BeanDefinitionReaderUtils.registerWithGeneratedName(sessionPollBuilder.getBeanDefinition(), parserContext.getRegistry());
		
		BeanDefinitionBuilder synchronizerBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.sftp.inbound.SftpInboundSynchronizer");
		synchronizerBuilder.addConstructorArgReference(sessionPollName);

		synchronizerBuilder.addPropertyValue("autoStartup", autoStartup);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "remote-directory", "remotePath");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "auto-delete-remote-files-on-sync", "shouldDeleteSourceFile");
		
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(synchronizerBuilder, element, "filter");

		BeanDefinitionBuilder messageSourceBuilder = 
			BeanDefinitionBuilder.rootBeanDefinition("org.springframework.integration.sftp.inbound.SftpInboundSynchronizingMessageSource");
		messageSourceBuilder.addPropertyValue("synchronizer", synchronizerBuilder.getBeanDefinition());

		if (hasFileNamePattern){
			if (parserContext.getRegistry().containsBeanDefinition(fileNamePattern)){
				messageSourceBuilder.addPropertyReference("filenamePattern", fileNamePattern);
			}
			else {
				messageSourceBuilder.addPropertyValue("filenamePattern", fileNamePattern);
			}
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "auto-create-directories");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "local-directory");

		return messageSourceBuilder.getBeanDefinition();
	}
}