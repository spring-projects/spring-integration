/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.IntegrationNamespaceUtils;
import org.springframework.integration.ftp.FtpSource;
import org.springframework.integration.ftp.QueuedFTPClientPool;

/**
 * Parser for the &lt;inbound-channel-adapter/&gt; element of the 'ftp' namespace.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public class FtpInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FtpSource.class);
		String username = element.getAttribute("username");
		String password = element.getAttribute("password");
		String host = element.getAttribute("host");
		String port = element.getAttribute("port");
		String remoteWorkingDirectory = element.getAttribute("remote-working-directory");
		QueuedFTPClientPool queuedFTPClientPool = new QueuedFTPClientPool();
		queuedFTPClientPool.setUsername(username);
		queuedFTPClientPool.setPassword(password);
		queuedFTPClientPool.setHost(host);
		queuedFTPClientPool.setPort(Integer.parseInt(port));
		queuedFTPClientPool.setRemoteWorkingDirectory(remoteWorkingDirectory);
		builder.addConstructorArgValue(queuedFTPClientPool);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "local-working-directory");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
