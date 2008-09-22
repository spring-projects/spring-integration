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

package org.springframework.integration.adapter.ftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.adapter.ftp.FtpSource;
import org.springframework.integration.adapter.ftp.QueuedFTPClientPool;

/**
 * Parser for the &lt;ftp-source/&gt; element.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public class FtpSourceParser extends AbstractDirectorySourceParser {

	private static final String POOL_ATTRIBUTE_USER = "username";

	private static final String POOL_ATTRIBUTE_PASS = "password";

	private static final String POOL_ATTRIBUTE_HOST = "host";

	private static final String POOL_ATTRIBUTE_PORT = "port";

	private static final String POOL_ATTRIBUTE_REMOTEDIR = "remote-working-directory";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return FtpSource.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !POOL_ATTRIBUTE_HOST.equals(attributeName) 
				&& !POOL_ATTRIBUTE_PASS.equals(attributeName)
				&& !POOL_ATTRIBUTE_PORT.equals(attributeName)
				&& !POOL_ATTRIBUTE_USER.equals(attributeName)
				&& !POOL_ATTRIBUTE_REMOTEDIR.equals(attributeName)
				&& super.isEligibleAttribute(attributeName);
	}
	
	@Override
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
		super.postProcess(beanDefinition, element);
		String user = element.getAttribute(POOL_ATTRIBUTE_USER);
		String pass = element.getAttribute(POOL_ATTRIBUTE_PASS);
		String host = element.getAttribute(POOL_ATTRIBUTE_HOST);
		String port = element.getAttribute(POOL_ATTRIBUTE_PORT);
		String remoteWorkingDirectory = element.getAttribute(POOL_ATTRIBUTE_REMOTEDIR);
		QueuedFTPClientPool queuedFTPClientPool = new QueuedFTPClientPool();
		queuedFTPClientPool.setUsername(user);
		queuedFTPClientPool.setPassword(pass);
		queuedFTPClientPool.setHost(host);
		queuedFTPClientPool.setPort(Integer.parseInt(port));
		queuedFTPClientPool.setRemoteWorkingDirectory(remoteWorkingDirectory);
		beanDefinition.addConstructorArgValue(queuedFTPClientPool);
	}

}
