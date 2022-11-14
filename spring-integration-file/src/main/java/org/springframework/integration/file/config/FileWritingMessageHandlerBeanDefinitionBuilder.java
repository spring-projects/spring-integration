/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.util.StringUtils;

/**
 * A common helper class for the 'outbound-channel-adapter' and 'outbound-gateway'
 * element parsers. Both of those are responsible for creating an instance of
 * {@link org.springframework.integration.file.FileWritingMessageHandler}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Tony Falabella
 * @author Gary Russell
 *
 * @since 1.0.3
 */
abstract class FileWritingMessageHandlerBeanDefinitionBuilder {

	static BeanDefinitionBuilder configure(Element element, boolean expectReply, ParserContext parserContext) {

		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(FileWritingMessageHandlerFactoryBean.class);

		String directory = element.getAttribute("directory");
		String directoryExpression = element.getAttribute("directory-expression");

		if (!StringUtils.hasText(directory) && !StringUtils.hasText(directoryExpression)) {
			parserContext.getReaderContext().error("directory or directory-expression is required", element);
		}
		else if (StringUtils.hasText(directory) && StringUtils.hasText(directoryExpression)) {
			parserContext.getReaderContext()
					.error("Either directory or directory-expression must be provided but not both", element);
		}

		if (StringUtils.hasText(directoryExpression)) {
			BeanDefinitionBuilder expressionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionBuilder.addConstructorArgValue(directoryExpression);
			builder.addPropertyValue("directoryExpression", expressionBuilder.getBeanDefinition());
		}

		builder.addPropertyValue("expectReply", expectReply);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "append-new-line");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-create-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delete-source-files");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "temporary-file-suffix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "mode", "fileExistsMode");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "buffer-size");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "flush-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "flush-when-idle");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "flush-predicate");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "chmod");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "preserve-timestamp");
		filenameGenerators(element, parserContext, builder);
		return builder;
	}

	private static void filenameGenerators(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {

		String remoteFileNameGenerator = element.getAttribute("filename-generator");
		String remoteFileNameGeneratorExpression = element.getAttribute("filename-generator-expression");
		boolean hasRemoteFileNameGenerator = StringUtils.hasText(remoteFileNameGenerator);
		boolean hasRemoteFileNameGeneratorExpression = StringUtils.hasText(remoteFileNameGeneratorExpression);
		if (hasRemoteFileNameGenerator || hasRemoteFileNameGeneratorExpression) {
			if (hasRemoteFileNameGenerator && hasRemoteFileNameGeneratorExpression) {
				parserContext.getReaderContext()
						.error("at most one of 'filename-generator-expression' or 'filename-generator' " +
								"is allowed on file outbound adapter/gateway", element);
			}
			if (hasRemoteFileNameGenerator) {
				builder.addPropertyReference("fileNameGenerator", remoteFileNameGenerator);
			}
			else {
				BeanDefinitionBuilder fileNameGeneratorBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(DefaultFileNameGenerator.class);
				fileNameGeneratorBuilder.addPropertyValue("expression", remoteFileNameGeneratorExpression);
				builder.addPropertyValue("fileNameGenerator", fileNameGeneratorBuilder.getBeanDefinition());
			}
		}
	}

}
