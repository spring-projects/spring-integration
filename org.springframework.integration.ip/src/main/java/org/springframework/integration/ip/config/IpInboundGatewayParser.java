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

package org.springframework.integration.ip.config;

import org.w3c.dom.Element;


import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.Conventions;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.integration.ip.tcp.SimpleTcpNetInboundGateway;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class IpInboundGatewayParser extends AbstractInboundGatewayParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return SimpleTcpNetInboundGateway.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !attributeName.equals(IpAdapterParserUtils.MESSAGE_FORMAT)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		builder.addPropertyValue(
				Conventions.attributeNameToPropertyName(IpAdapterParserUtils.MESSAGE_FORMAT), 
				IpAdapterParserUtils.getMessageFormat(element));
	}

}
