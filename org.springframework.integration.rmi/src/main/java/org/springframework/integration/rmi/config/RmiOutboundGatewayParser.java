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
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.config.AbstractRemotingOutboundGatewayParser;
import org.springframework.integration.rmi.RmiInboundGateway;
import org.springframework.integration.rmi.RmiOutboundGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-gateway/&gt; element of the 'rmi' namespace. 
 * 
 * @author Mark Fisher
 */
public class RmiOutboundGatewayParser extends AbstractRemotingOutboundGatewayParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return RmiOutboundGateway.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !"host".equals(attributeName)
				&& !"port".equals(attributeName)
				&& !"remote-channel".equals(attributeName)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		String host = element.getAttribute("host");
		String remoteChannel = element.getAttribute("remote-channel");
		if (!(StringUtils.hasText(host) && StringUtils.hasText(remoteChannel))) {
			throw new ConfigurationException("The 'host' and 'remote-channel' attributes are both required");
		}
		String portAttribute = element.getAttribute("port");
		String port = StringUtils.hasText(portAttribute) ? portAttribute : "" + Registry.REGISTRY_PORT;
		String url = "rmi://" + host + ":" + port + "/" + RmiInboundGateway.SERVICE_NAME_PREFIX + remoteChannel;
		builder.addConstructorArgValue(url);
	}

}
