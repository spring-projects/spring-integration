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

import org.springframework.integration.adapter.config.AbstractRemotingOutboundGatewayParser;
import org.springframework.integration.rmi.RmiInboundGateway;
import org.springframework.integration.rmi.RmiOutboundGateway;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-gateway/&gt; element of the 'rmi' namespace. 
 * 
 * @author Mark Fisher
 */
public class RmiOutboundGatewayParser extends AbstractRemotingOutboundGatewayParser {

	@Override
	protected Class<?> getGatewayClass(Element element) {
		return RmiOutboundGateway.class;
	}

	@Override
	protected String parseUrl(Element element) {
		String host = element.getAttribute("host");
		String remoteChannel = element.getAttribute("remote-channel");
		Assert.isTrue(StringUtils.hasText(host) && StringUtils.hasText(remoteChannel),
				"The 'host' and 'remote-channel' attributes are both required");
		String portAttribute = element.getAttribute("port");
		String port = StringUtils.hasText(portAttribute) ? portAttribute : "" + Registry.REGISTRY_PORT;
		return "rmi://" + host + ":" + port + "/" + RmiInboundGateway.SERVICE_NAME_PREFIX + remoteChannel;
	}

}
