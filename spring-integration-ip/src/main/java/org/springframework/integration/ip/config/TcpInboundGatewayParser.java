/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class TcpInboundGatewayParser extends AbstractInboundGatewayParser {

	private static final String BASE_PACKAGE = "org.springframework.integration.ip.tcp";

	@Override
	protected String getBeanClassName(Element element) {
		return BASE_PACKAGE + ".TcpInboundGateway";
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !attributeName.equals(IpAdapterParserUtils.TCP_CONNECTION_FACTORY)
				&& !attributeName.equals(IpAdapterParserUtils.SCHEDULER)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.TCP_CONNECTION_FACTORY);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.SCHEDULER, "taskScheduler");
	}

}
