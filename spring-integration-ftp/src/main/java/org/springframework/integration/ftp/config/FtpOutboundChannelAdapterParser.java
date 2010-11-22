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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class FtpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.file.remote.handler.FileTransferringMessageHandler");
		BeanDefinitionBuilder sessionFactoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.file.remote.session.CachingSessionFactory");
		sessionFactoryBuilder.addConstructorArgReference(element.getAttribute("session-factory"));
		handlerBuilder.addConstructorArgValue(sessionFactoryBuilder.getBeanDefinition());
		String remoteDirectory = element.getAttribute("remote-directory");
		String remoteDirectoryExpression = element.getAttribute("remote-directory-expression");
		boolean hasDirectory = StringUtils.hasText(remoteDirectory);
		boolean hasDirectoryExpression = StringUtils.hasText(remoteDirectoryExpression);
		if (!(hasDirectory ^ hasDirectoryExpression)) {
			throw new BeanDefinitionStoreException("exactly one of 'remote-directory' or 'remote-directory-expression' " +
					"is required on the FTP outbound adapter");
		}
		BeanDefinition expressionDef = null;
		if (hasDirectory) {
			expressionDef = new RootBeanDefinition("org.springframework.expression.common.LiteralExpression");
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(remoteDirectory);
		}
		else if (hasDirectoryExpression) {
			expressionDef = new RootBeanDefinition("org.springframework.integration.config.ExpressionFactoryBean");	
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(remoteDirectoryExpression);
		}
		handlerBuilder.addPropertyValue("remoteDirectoryExpression", expressionDef);
		String remoteFileExpression = element.getAttribute("remote-filename-generator-expression");
		String fileNameGenerator = element.getAttribute("remote-filename-generator");
		boolean hasRemoteFileExpression = StringUtils.hasText(remoteFileExpression);
		boolean hasFileNameGenerator = StringUtils.hasText(fileNameGenerator);
		if (hasRemoteFileExpression || hasFileNameGenerator) {
			if (hasRemoteFileExpression && hasFileNameGenerator) {
				throw new BeanDefinitionStoreException("at most one of 'remote-filename-generator-expression' or 'remote-filename-generator' " +
						"is allowed on the FTP outbound adapter");
			}
			if (StringUtils.hasText(remoteFileExpression)) {
				BeanDefinitionBuilder fileNameGeneratorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
						"org.springframework.integration.file.DefaultFileNameGenerator");
				fileNameGeneratorBuilder.addPropertyValue("expression", remoteFileExpression);
				handlerBuilder.addPropertyValue("fileNameGenerator", fileNameGeneratorBuilder.getBeanDefinition());
			}
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(handlerBuilder, element, "remote-filename-generator", "fileNameGenerator");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(handlerBuilder, element, "charset");
		return handlerBuilder.getBeanDefinition();
	}

}
