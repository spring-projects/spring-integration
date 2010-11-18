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
 * Parser for 'sftp:outbound-channel-adapter'
 *
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SftpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder sessionPollBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.sftp.session.QueuedSftpSessionPool");
		sessionPollBuilder.addConstructorArgReference(element.getAttribute("session-factory"));
		
		BeanDefinitionBuilder handlerBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.sftp.outbound.SftpSendingMessageHandler");
		handlerBuilder.addConstructorArgValue(sessionPollBuilder.getBeanDefinition());
		IntegrationNamespaceUtils.setValueIfAttributeDefined(handlerBuilder, element, "charset");
		
		String remoteDirectory = element.getAttribute("remote-directory");
		String remoteDirectoryExpression = element.getAttribute("remote-directory-expression");
		boolean hasDirectory = StringUtils.hasText(remoteDirectory);
		boolean hasDirectoryExpression = StringUtils.hasText(remoteDirectoryExpression);
		if (!(hasDirectory ^ hasDirectoryExpression)) {
			throw new BeanDefinitionStoreException("exactly one of 'remote-directory' or 'remote-directory-expression' " +
					"is required on SFTP outbound adapter");
		}
		BeanDefinition expressionDef = null;
		if (hasDirectory){
			expressionDef = new RootBeanDefinition("org.springframework.expression.common.LiteralExpression");
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(remoteDirectory);
		}
		else if (hasDirectoryExpression){
			expressionDef = new RootBeanDefinition("org.springframework.integration.config.ExpressionFactoryBean");	
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(remoteDirectoryExpression);
		}
		handlerBuilder.addPropertyValue("remoteDirectoryExpression", expressionDef);
	
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(handlerBuilder, element, "filename-generator");
		return handlerBuilder.getBeanDefinition();
	}
}