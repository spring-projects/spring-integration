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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Parser for the FTPS inbound-channel-adapter
 *
 * @author Josh Long
 */
public class FtpsInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	private Set<String> receiveAttrs = new HashSet<String>(Arrays.asList(
			"auto-delete-remote-files-on-sync,filename-pattern,local-working-directory".split(",")));

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				FtpsRemoteFileSystemSynchronizingMessageSourceFactoryBean.class.getName());
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "filter");
		for (String a : receiveAttrs) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, a);
		}
		FtpNamespaceParserSupport.configureCoreFtpClient(builder, element, parserContext);
		return builder.getBeanDefinition();
	}

}
