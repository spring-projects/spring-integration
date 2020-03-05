/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.integration.rsocket.config;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.rsocket.inbound.RSocketInboundGateway;

/**
 * Parser for the &lt;inbound-gateway/&gt; element of the 'rsocket' namespace.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketInboundGatewayParser extends AbstractInboundGatewayParser {

	private static final List<String> NON_ELIGIBLE_ATTRIBUTES =
			Arrays.asList("path",
					"rsocket-strategies",
					"rsocket-connector",
					"request-element-type");

	@Override
	protected Class<?> getBeanClass(Element element) {
		return RSocketInboundGateway.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !NON_ELIGIBLE_ATTRIBUTES.contains(attributeName) && super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		builder.addConstructorArgValue(element.getAttribute("path"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-element-type",
				"requestElementClass");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "rsocket-strategies",
				"rSocketStrategies");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "rsocket-connector",
				"RSocketConnector");
	}

}
