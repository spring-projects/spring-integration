/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
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
 * @since 1.0.3
 */
abstract class FileWritingMessageHandlerBeanDefinitionBuilder {

	static BeanDefinitionBuilder configure(Element element, boolean expectReply, ParserContext parserContext) {

		String directory = element.getAttribute("directory");
		if (!StringUtils.hasText(directory)) {
			parserContext.getReaderContext().error("directory is required", element);
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FileWritingMessageHandlerFactoryBean.class);
		builder.addPropertyValue("directory", directory);
		builder.addPropertyValue("expectReply", expectReply);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-create-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delete-source-files");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "temporary-file-suffix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "append-if-exists");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		String remoteFileNameGenerator = element.getAttribute("filename-generator");
		String remoteFileNameGeneratorExpression = element.getAttribute("filename-generator-expression");
		boolean hasRemoteFileNameGenerator = StringUtils.hasText(remoteFileNameGenerator);
		boolean hasRemoteFileNameGeneratorExpression = StringUtils.hasText(remoteFileNameGeneratorExpression);
		if (hasRemoteFileNameGenerator || hasRemoteFileNameGeneratorExpression) {
			if (hasRemoteFileNameGenerator && hasRemoteFileNameGeneratorExpression) {
				parserContext.getReaderContext().error("at most one of 'filename-generator-expression' or 'filename-generator' " +
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
		return builder;
	}

}
