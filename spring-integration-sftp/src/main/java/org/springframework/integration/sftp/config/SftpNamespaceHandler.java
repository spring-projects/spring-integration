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

package org.springframework.integration.sftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Provides namespace support for using SFTP.
 * This is largely based on the FTP support by Iwein Fuld.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg ZHurakousky
 * @since 2.0
 */
public class SftpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new SftpInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new SftpOutboundChannelAdapterParser());
	}


	/**
	 * Configures an object that can receive files from a remote SFTP endpoint and broadcast their arrival to a channel.
	 */
	private static class SftpInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

		@Override
		protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
			BeanDefinitionBuilder builder = 
				BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.sftp.config.SftpInboundSynchronizingMessageSourceFactoryBean");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "filter");
			for (String p : "auto-startup,filename-pattern,auto-create-directories,remote-directory,local-directory-path,auto-delete-remote-files-on-sync".split(",")) {
				IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, p);
			}
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "session-factory", "sftpSessionFactory");
			return builder.getBeanDefinition();
		}
	}


	/**
	 * Configures an object that can take messages and send them via SFTP.
	 */
	private static class SftpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

		@Override
		protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
			BeanDefinitionBuilder builder = 
				BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.sftp.config.SftpSendingMessageHandlerFactoryBean");
			for (String p : "charset".split(",")) {
				IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, p);
			}
			String remoteDirectory = element.getAttribute("remote-directory");
			String remoteDirectoryExpression = element.getAttribute("remote-directory-expression");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "session-factory", "sftpSessionFactory");
			boolean hasLiteralRemoteDirectory = StringUtils.hasText(remoteDirectory);
			boolean hasRemoteDirectoryExpression = StringUtils.hasText(remoteDirectoryExpression);
			if (hasLiteralRemoteDirectory ^ hasRemoteDirectoryExpression) {
				if (hasLiteralRemoteDirectory) {
					builder.addPropertyValue("remoteDirectory", remoteDirectory);
				}
				else {
					BeanDefinitionBuilder expressionDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(
							"org.springframework.integration.config.ExpressionFactoryBean");
					expressionDefBuilder.addConstructorArgValue(remoteDirectoryExpression);
					builder.addPropertyValue("remoteDirectoryExpression", expressionDefBuilder.getBeanDefinition());
				}
			}
			else {
				parserContext.getReaderContext().error("exactly one of 'remote-directory' or 'remote-directory-expression' is required", element);
			}
			return builder.getBeanDefinition();
		}
	}

}
