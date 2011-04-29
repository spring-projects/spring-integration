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

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for parsing remote file inbound channel adapters.
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractRemoteFileInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected final BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder synchronizerBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				this.getInboundFileSynchronizerClassname());

		// build the SessionFactory and provide as a constructor argument
		String cacheSessions = element.getAttribute("cache-sessions");
		if ("false".equalsIgnoreCase(cacheSessions)) {
			synchronizerBuilder.addConstructorArgReference(element.getAttribute("session-factory"));
		}
		else {
			BeanDefinitionBuilder sessionFactoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.integration.file.remote.session.CachingSessionFactory");
			sessionFactoryBuilder.addConstructorArgReference(element.getAttribute("session-factory"));
			synchronizerBuilder.addConstructorArgValue(sessionFactoryBuilder.getBeanDefinition());
		}

		// configure the InboundFileSynchronizer properties
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "remote-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "delete-remote-files");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "remote-file-separator");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "temporary-file-suffix");
		this.configureFilter(synchronizerBuilder, element, parserContext);

		// build the MessageSource
		BeanDefinitionBuilder messageSourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(this.getMessageSourceClassname());
		messageSourceBuilder.addConstructorArgValue(synchronizerBuilder.getBeanDefinition());
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "auto-create-local-directory");
		return messageSourceBuilder.getBeanDefinition();
	}

	private void configureFilter(BeanDefinitionBuilder synchronizerBuilder, Element element, ParserContext parserContext) {
		String filter = element.getAttribute("filter");
		String fileNamePattern = element.getAttribute("filename-pattern");
		String fileNameRegex = element.getAttribute("filename-regex");
		boolean hasFilter = StringUtils.hasText(filter);
		boolean hasFileNamePattern = StringUtils.hasText(fileNamePattern);
		boolean hasFileNameRegex = StringUtils.hasText(fileNameRegex);
		if (hasFilter || hasFileNamePattern || hasFileNameRegex) {
			int count = 0;
			if (hasFilter) {
				count++;
			}
			if (hasFileNamePattern) {
				count++;
			}
			if (hasFileNameRegex) {
				count++;
			}
			if (count != 1) {
				parserContext.getReaderContext().error("at most one of 'filename-pattern', " +
						"'filename-regex', or 'filter' is allowed on remote file inbound adapter", element);
			}
			if (hasFilter) {
				synchronizerBuilder.addPropertyReference("filter", filter);
			}
			else if (hasFileNamePattern) {
				BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
						this.getSimplePatternFileListFilterClassname());
				filterBuilder.addConstructorArgValue(fileNamePattern);
				synchronizerBuilder.addPropertyValue("filter", filterBuilder.getBeanDefinition());
			}
			else if (hasFileNameRegex) {
				BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
						this.getRegexPatternFileListFilterClassname());
				filterBuilder.addConstructorArgValue(fileNameRegex);
				synchronizerBuilder.addPropertyValue("filter", filterBuilder.getBeanDefinition());
			}
		}
	}

	protected abstract String getMessageSourceClassname();

	protected abstract String getInboundFileSynchronizerClassname();

	protected abstract String getSimplePatternFileListFilterClassname();

	protected abstract String getRegexPatternFileListFilterClassname();

}
