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

package org.springframework.integration.rmi.config;

import java.rmi.registry.Registry;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.rmi.RmiGateway;
import org.springframework.integration.rmi.RmiHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;rmi-handler/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class RmiHandlerParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return RmiHandler.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String host = element.getAttribute("host");
		String remoteChannel = element.getAttribute("remote-channel");
		if (!(StringUtils.hasText(host) && StringUtils.hasText(remoteChannel))) {
			throw new ConfigurationException("The 'host' and 'remote-channel' attributes are both required");
		}
		String portAttribute = element.getAttribute("port");
		String port = StringUtils.hasText(portAttribute) ? portAttribute : "" + Registry.REGISTRY_PORT;
		String url = "rmi://" + host + ":" + port + "/" + RmiGateway.SERVICE_NAME_PREFIX + remoteChannel;
		builder.addConstructorArgValue(url);
	}

}
