/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.rmi.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Parser for the &lt;inbound-gateway/&gt; element of the 'rmi' namespace.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @deprecated since 5.4 with no replacement.
 */
@Deprecated
public class RmiInboundGatewayParser extends AbstractInboundGatewayParser {

	private static final String REMOTE_INVOCATION_EXECUTOR_ATTRIBUTE = "remote-invocation-executor";


	@Override
	protected String getBeanClassName(Element element) {
		return org.springframework.integration.rmi.RmiInboundGateway.class.getName();
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !attributeName.equals(REMOTE_INVOCATION_EXECUTOR_ATTRIBUTE)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, REMOTE_INVOCATION_EXECUTOR_ATTRIBUTE);
	}

}
