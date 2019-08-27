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

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.synchronizer.InboundFileSynchronizer;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for parsing remote file inbound channel adapters.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Venil Noronha
 *
 * @since 2.0
 */
public abstract class AbstractRemoteFileInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected final BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder synchronizerBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(getInboundFileSynchronizerClass());

		synchronizerBuilder.addConstructorArgReference(element.getAttribute("session-factory"));

		// configure the InboundFileSynchronizer properties
		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression(
						"remote-directory", "remote-directory-expression", parserContext, element, false);
		if (expressionDef != null) {
			synchronizerBuilder.addPropertyValue("remoteDirectoryExpression", expressionDef);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "delete-remote-files");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "preserve-timestamp");

		String remoteFileSeparator = element.getAttribute("remote-file-separator");
		synchronizerBuilder.addPropertyValue("remoteFileSeparator", remoteFileSeparator);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "temporary-file-suffix");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(synchronizerBuilder, element,
				"remote-file-metadata-store");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(synchronizerBuilder, element, "metadata-store-prefix");

		FileParserUtils.configureFilter(synchronizerBuilder, element, parserContext,
				getSimplePatternFileListFilterClass(), getRegexPatternFileListFilterClass(),
				getPersistentAcceptOnceFileListFilterClass());

		// build the MessageSource
		BeanDefinitionBuilder messageSourceBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(getMessageSourceClassname());
		messageSourceBuilder.addConstructorArgValue(synchronizerBuilder.getBeanDefinition());
		String comparator = element.getAttribute("comparator");
		if (StringUtils.hasText(comparator)) {
			messageSourceBuilder.addConstructorArgReference(comparator);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(messageSourceBuilder, element, "local-filter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(messageSourceBuilder, element, "scanner");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element,
				"auto-create-local-directory");
		String localFileGeneratorExpression = element.getAttribute("local-filename-generator-expression");
		if (StringUtils.hasText(localFileGeneratorExpression)) {
			BeanDefinitionBuilder localFileGeneratorExpressionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			localFileGeneratorExpressionBuilder.addConstructorArgValue(localFileGeneratorExpression);
			synchronizerBuilder.addPropertyValue("localFilenameGeneratorExpression",
					localFileGeneratorExpressionBuilder.getBeanDefinition());
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageSourceBuilder, element, "max-fetch-size");
		return messageSourceBuilder.getBeanDefinition();
	}

	protected abstract String getMessageSourceClassname();

	protected abstract Class<? extends InboundFileSynchronizer> getInboundFileSynchronizerClass();

	protected abstract Class<? extends FileListFilter<?>> getSimplePatternFileListFilterClass();

	protected abstract Class<? extends FileListFilter<?>> getRegexPatternFileListFilterClass();

	protected abstract Class<? extends AbstractPersistentAcceptOnceFileListFilter<?>>
	getPersistentAcceptOnceFileListFilterClass();

}
