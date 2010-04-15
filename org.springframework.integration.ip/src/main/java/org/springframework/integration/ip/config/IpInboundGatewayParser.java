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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.Conventions;
import org.springframework.integration.adapter.config.AbstractRemotingGatewayParser;
import org.springframework.integration.ip.tcp.SimpleTcpNetInboundGateway;
import org.w3c.dom.Element;

import sun.print.IPPPrintService;

/**
 * @author Gary Russell
 *
 */
public class IpInboundGatewayParser extends AbstractRemotingGatewayParser {

	
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser#getBeanClass(org.w3c.dom.Element)
	 */
	@Override
	protected Class getBeanClass(Element element) {
		return SimpleTcpNetInboundGateway.class;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.adapter.config.AbstractRemotingGatewayParser#isEligibleAttribute(java.lang.String)
	 */
	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !attributeName.equals(IpAdapterParserUtils.MESSAGE_FORMAT)
				&& super.isEligibleAttribute(attributeName);
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.adapter.config.AbstractRemotingGatewayParser#doPostProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.w3c.dom.Element)
	 */
	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		builder.addPropertyValue(
				Conventions.attributeNameToPropertyName(IpAdapterParserUtils.MESSAGE_FORMAT), 
				IpAdapterParserUtils.getMessageFormat(element));
	}


}
