/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
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

		builder.addConstructorArgValue(element.getAttribute("command"));
		builder.addConstructorArgValue(element.getAttribute(EXPRESSION_ATTRIBUTE));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "command-options", "options");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		this.configureFilter(builder, element, parserContext, "filter", "filename", "filter");
		this.configureFilter(builder, element, parserContext, "mput-filter", "mput", "mputFilter");

		BeanDefinition localDirExpressionDef = IntegrationNamespaceUtils
				.createExpressionDefinitionFromValueOrExpression("local-directory", "local-directory-expression",
						parserContext, element, false);
		builder.addPropertyValue("localDirectoryExpression", localDirExpressionDef);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-create-local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "order");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "rename-expression");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		String localFileGeneratorExpression = element.getAttribute("local-filename-generator-expression");
		if (StringUtils.hasText(localFileGeneratorExpression)) {
			BeanDefinitionBuilder localFileGeneratorExpressionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			localFileGeneratorExpressionBuilder.addConstructorArgValue(localFileGeneratorExpression);
			builder.addPropertyValue("localFilenameGeneratorExpression", localFileGeneratorExpressionBuilder.getBeanDefinition());
		}
		return builder;
	}

	protected void configureFilter(BeanDefinitionBuilder builder, Element element, ParserContext parserContext,
			String filterAttribute, String patternPrefix, String propertyName) {
		String filter = element.getAttribute(filterAttribute);
		String fileNamePattern = element.getAttribute(patternPrefix + "-pattern");
		String fileNameRegex = element.getAttribute(patternPrefix + "-regex");
		boolean hasFilter = StringUtils.hasText(filter);
		boolean hasFileNamePattern = StringUtils.hasText(fileNamePattern);
		boolean hasFileNameRegex = StringUtils.hasText(fileNameRegex);
		int count = hasFilter ? 1 : 0;
		count += hasFileNamePattern ? 1 : 0;
		count += hasFileNameRegex ? 1 : 0;
		if (count > 1) {
			parserContext.getReaderContext().error("at most one of '" + patternPrefix + "-pattern', " +
					"'" + patternPrefix + "-regex', or '" + filterAttribute + "' is allowed on a remote file outbound gateway", element);
		}
		else if (hasFilter) {
			builder.addPropertyReference(propertyName, filter);
		}
		else if (hasFileNamePattern) {
			BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					"filter".equals(filterAttribute) ?
							this.getSimplePatternFileListFilterClassName() :
							SimplePatternFileListFilter.class.getName());
			filterBuilder.addConstructorArgValue(fileNamePattern);
			builder.addPropertyValue(propertyName, filterBuilder.getBeanDefinition());
		}
		else if (hasFileNameRegex) {
			BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					"filter".equals(filterAttribute) ?
							this.getRegexPatternFileListFilterClassName() :
							RegexPatternFileListFilter.class.getName());
			filterBuilder.addConstructorArgValue(fileNameRegex);
			builder.addPropertyValue(propertyName, filterBuilder.getBeanDefinition());
		}
	}

	protected abstract String getRegexPatternFileListFilterClassName();

	protected abstract String getSimplePatternFileListFilterClassName();

	protected abstract String getGatewayClassName();

	protected abstract Class<? extends RemoteFileOperations<?>> getTemplateClass();

}
