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
package org.springframework.integration.ftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
public abstract class AbstractFtpInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder messageSourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(this.getClassName());

		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "auto-create-directories");
		
		BeanDefinitionBuilder poolBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.file.remote.session.CachingSessionFactory");
		poolBuilder.addConstructorArgReference(element.getAttribute("session-factory"));
		
		BeanDefinitionBuilder synchronizerBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer");
		
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "remote-directory", "remotePath");
		synchronizerBuilder.addPropertyValue("sessionFactory", poolBuilder.getBeanDefinition());
//		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "auto-delete-remote-files-on-sync", "shouldDeleteSourceFile");
//		
//		
		String fileNamePattern = element.getAttribute("filename-pattern");
		String filter = element.getAttribute("filter");
		boolean hasFileNamePattern = StringUtils.hasText(fileNamePattern);
		boolean hasFilter = StringUtils.hasText(filter);
		if (hasFileNamePattern || hasFilter) {
			if (!(hasFileNamePattern ^ hasFilter)) {
				throw new BeanDefinitionStoreException("at most one of 'filename-pattern' or 'filter' " +
						"is allowed on FTP inbound adapter");
			}
		}
		
		if (hasFileNamePattern){
			BeanDefinitionBuilder filterBuilder = 
				BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.ftp.filters.FtpPatternMatchingFileListFilter");
			filterBuilder.addConstructorArgValue(fileNamePattern);
			synchronizerBuilder.addPropertyValue("filter", filterBuilder.getBeanDefinition());
		} 
		else if (hasFilter) {
			synchronizerBuilder.addPropertyReference("filter", filter);
		}
//		
		messageSourceBuilder.addPropertyValue("synchronizer", synchronizerBuilder.getBeanDefinition());
		
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "local-working-directory", "localDirectory");
		
		return messageSourceBuilder.getBeanDefinition();
	}
	
	protected abstract String getClassName();
}
