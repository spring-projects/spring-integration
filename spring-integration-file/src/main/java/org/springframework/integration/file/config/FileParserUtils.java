/*
 * Copyright 2013-2014 the original author or authors.
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
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @since 3.0
 *
 */
public final class FileParserUtils {

	private FileParserUtils() {
	}

	public static BeanDefinition parseRemoteFileTemplate(Element element, ParserContext parserContext,
			boolean atLeastOneRemoteDirectoryAttributeRequired, Class<? extends RemoteFileOperations<?>> templateClass) {
		BeanDefinitionBuilder templateBuilder = BeanDefinitionBuilder.genericBeanDefinition(templateClass);

		templateBuilder.addConstructorArgReference(element.getAttribute("session-factory"));
		// configure MessageHandler properties

		IntegrationNamespaceUtils.setValueIfAttributeDefined(templateBuilder, element, "temporary-file-suffix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(templateBuilder, element, "use-temporary-file-name");

		IntegrationNamespaceUtils.setValueIfAttributeDefined(templateBuilder, element, "auto-create-directory");

		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("remote-directory",
						"remote-directory-expression", parserContext, element, atLeastOneRemoteDirectoryAttributeRequired);
		if (expressionDef != null) {
			templateBuilder.addPropertyValue("remoteDirectoryExpression", expressionDef);
		}
		expressionDef = IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("temporary-remote-directory",
				"temporary-remote-directory-expression", parserContext, element, false);
		if (expressionDef != null) {
			templateBuilder.addPropertyValue("temporaryRemoteDirectoryExpression", expressionDef);
		}

		// configure remote FileNameGenerator
		String remoteFileNameGenerator = element.getAttribute("remote-filename-generator");
		String remoteFileNameGeneratorExpression = element.getAttribute("remote-filename-generator-expression");
		boolean hasRemoteFileNameGenerator = StringUtils.hasText(remoteFileNameGenerator);
		boolean hasRemoteFileNameGeneratorExpression = StringUtils.hasText(remoteFileNameGeneratorExpression);
		if (hasRemoteFileNameGenerator || hasRemoteFileNameGeneratorExpression) {
			if (hasRemoteFileNameGenerator && hasRemoteFileNameGeneratorExpression) {
				parserContext.getReaderContext().error(
						"at most one of 'remote-filename-generator-expression' or 'remote-filename-generator' "
								+ "is allowed on a remote file outbound adapter", element);
			}
			if (hasRemoteFileNameGenerator) {
				templateBuilder.addPropertyReference("fileNameGenerator", remoteFileNameGenerator);
			}
			else {
				BeanDefinitionBuilder fileNameGeneratorBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(DefaultFileNameGenerator.class);
				fileNameGeneratorBuilder.addPropertyValue("expression", remoteFileNameGeneratorExpression);
				templateBuilder.addPropertyValue("fileNameGenerator", fileNameGeneratorBuilder.getBeanDefinition());
			}
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(templateBuilder, element, "charset");
		templateBuilder.addPropertyValue("remoteFileSeparator", element.getAttribute("remote-file-separator"));
		return templateBuilder.getBeanDefinition();
	}

}
