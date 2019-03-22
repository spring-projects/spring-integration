/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for parsing remote file streaming inbound channel adapters.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 */
public abstract class AbstractRemoteFileStreamingInboundChannelAdapterParser
		extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected final BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinition templateDefinition = FileParserUtils.parseRemoteFileTemplate(element, parserContext, false,
				getTemplateClass());

		BeanDefinitionBuilder messageSourceBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(getMessageSourceClass());
		messageSourceBuilder.addConstructorArgValue(templateDefinition);

		BeanDefinition expressionDef = IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression(
				"remote-directory", "remote-directory-expression", parserContext, element, false);
		if (expressionDef != null) {
			messageSourceBuilder.addPropertyValue("remoteDirectoryExpression", expressionDef);
		}

		String remoteFileSeparator = element.getAttribute("remote-file-separator");
		messageSourceBuilder.addPropertyValue("remoteFileSeparator", remoteFileSeparator);
		FileParserUtils.configureFilter(messageSourceBuilder, element, parserContext,
				getSimplePatternFileListFilterClass(), getRegexPatternFileListFilterClass(), getPersistentAcceptOnceFileListFilterClass());

		String comparator = element.getAttribute("comparator");
		if (StringUtils.hasText(comparator)) {
			messageSourceBuilder.addConstructorArgReference(comparator);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "max-fetch-size");
		return messageSourceBuilder.getBeanDefinition();
	}

	protected abstract Class<? extends RemoteFileOperations<?>> getTemplateClass();

	protected abstract Class<? extends MessageSource<?>> getMessageSourceClass();

	protected abstract Class<? extends FileListFilter<?>> getSimplePatternFileListFilterClass();

	protected abstract Class<? extends FileListFilter<?>> getRegexPatternFileListFilterClass();

	protected abstract Class<? extends AbstractPersistentAcceptOnceFileListFilter<?>> getPersistentAcceptOnceFileListFilterClass();

}
