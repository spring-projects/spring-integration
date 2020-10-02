/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.util.StringUtils;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 */
public abstract class AbstractRemoteFileOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinition templateDefinition = FileParserUtils.parseRemoteFileTemplate(element, parserContext, false,
				getTemplateClass());

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(getGatewayClassName());

		builder.addConstructorArgValue(templateDefinition);

		if (element.hasAttribute("session-callback")) {
			builder.addConstructorArgReference(element.getAttribute("session-callback"));
		}
		else {
			builder.addConstructorArgValue(element.getAttribute("command"));
			builder.addConstructorArgValue(element.getAttribute(EXPRESSION_ATTRIBUTE));
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "command-options", "options");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		configureFilter(builder, element, parserContext, FileParserUtils.FILTER_ATTRIBUTE, "filename",
				FileParserUtils.FILTER_ATTRIBUTE);
		configureFilter(builder, element, parserContext, "mput-filter", "mput", "mputFilter");

		BeanDefinition localDirExpressionDef = IntegrationNamespaceUtils
				.createExpressionDefinitionFromValueOrExpression("local-directory", "local-directory-expression",
						parserContext, element, false);
		builder.addPropertyValue("localDirectoryExpression", localDirExpressionDef);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-create-local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "order");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "rename-expression",
				"renameExpressionString");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "local-filename-generator-expression",
				"localFilenameGeneratorExpressionString");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "mode", "fileExistsMode");
		postProcessBuilder(builder, element);
		return builder;
	}

	protected void postProcessBuilder(BeanDefinitionBuilder builder, Element element) {
		// no-op
	}

	protected void configureFilter(BeanDefinitionBuilder builder, Element element, ParserContext parserContext,
			String filterAttribute, String patternPrefix, String propertyName) {

		String filter = element.getAttribute(filterAttribute);
		String filterExpression = element.getAttribute(filterAttribute + "-expression");
		String fileNamePattern = element.getAttribute(patternPrefix + "-pattern");
		String fileNameRegex = element.getAttribute(patternPrefix + "-regex");
		boolean hasFilter = StringUtils.hasText(filter);
		boolean hasFilterExpression = StringUtils.hasText(filterExpression);
		boolean hasFileNamePattern = StringUtils.hasText(fileNamePattern);
		boolean hasFileNameRegex = StringUtils.hasText(fileNameRegex);
		int count = hasFilter ? 1 : 0;
		count += hasFilterExpression ? 1 : 0;
		count += hasFileNamePattern ? 1 : 0;
		count += hasFileNameRegex ? 1 : 0;
		if (count > 1) {
			parserContext.getReaderContext()
					.error("at most one of '" + patternPrefix + "-pattern', " +
									"'" + patternPrefix + "-regex', '" + filterAttribute +
									"' or '" + filterAttribute + "-expression' is allowed on a remote file outbound " +
									"gateway",
							element);
		}
		else if (hasFilter) {
			builder.addPropertyReference(propertyName, filter);
		}
		else if (hasFilterExpression) {
			registerExpressionFilter(builder, propertyName, filterExpression);
		}
		else if (hasFileNamePattern) {
			registerPatternFilter(builder, filterAttribute, propertyName, fileNamePattern);
		}
		else if (hasFileNameRegex) {
			registerRegexFilter(builder, filterAttribute, propertyName, fileNameRegex);
		}
	}

	private void registerRegexFilter(BeanDefinitionBuilder builder, String filterAttribute, String propertyName,
			String fileNameRegex) {

		BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				FileParserUtils.FILTER_ATTRIBUTE.equals(filterAttribute) ?
						getRegexPatternFileListFilterClassName() :
						RegexPatternFileListFilter.class.getName());
		filterBuilder.addConstructorArgValue(fileNameRegex);
		builder.addPropertyValue(propertyName, filterBuilder.getBeanDefinition());
	}

	private void registerPatternFilter(BeanDefinitionBuilder builder, String filterAttribute, String propertyName,
			String fileNamePattern) {

		BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				FileParserUtils.FILTER_ATTRIBUTE.equals(filterAttribute) ?
						getSimplePatternFileListFilterClassName() :
						SimplePatternFileListFilter.class.getName());
		filterBuilder.addConstructorArgValue(fileNamePattern);
		builder.addPropertyValue(propertyName, filterBuilder.getBeanDefinition());
	}

	private void registerExpressionFilter(BeanDefinitionBuilder builder, String propertyName,
			String filterExpression) {

		BeanDefinition expressionFilterBeanDefinition =
				BeanDefinitionBuilder.genericBeanDefinition(ExpressionFileListFilter.class)
						.addConstructorArgValue(filterExpression)
						.getBeanDefinition();
		builder.addPropertyValue(propertyName, expressionFilterBeanDefinition);
	}

	protected abstract String getRegexPatternFileListFilterClassName();

	protected abstract String getSimplePatternFileListFilterClassName();

	protected abstract String getGatewayClassName();

	protected abstract Class<? extends RemoteFileOperations<?>> getTemplateClass();

}
