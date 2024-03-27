/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.ws.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ws.DefaultSoapHeaderMapper;
import org.springframework.util.StringUtils;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class WebServiceInboundGatewayParser extends AbstractInboundGatewayParser {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	@Override
	protected String getBeanClassName(Element element) {
		String simpleClassName = (StringUtils.hasText(element.getAttribute("marshaller"))) ?
				"MarshallingWebServiceInboundGateway" : "SimpleWebServiceInboundGateway";
		return "org.springframework.integration.ws." + simpleClassName;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !(attributeName.endsWith("marshaller")) &&
				!(attributeName.equals("mapped-reply-headers")) &&
				!(attributeName.equals("mapped-request-headers")) &&
				super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		String marshallerRef = element.getAttribute("marshaller");
		String unmarshallerRef = element.getAttribute("unmarshaller");
		if (StringUtils.hasText(marshallerRef)) {
			builder.addConstructorArgReference(marshallerRef);
			if (StringUtils.hasText(unmarshallerRef)) {
				builder.addConstructorArgReference(unmarshallerRef);
			}
		}
		else { // check if unmarshaller is defined which is a mistake without marshaller
			if (StringUtils.hasText(unmarshallerRef)) {
				throw new IllegalArgumentException("An 'unmarshaller' is not allowed without 'marshaller'.");
			}
		}

		if (StringUtils.hasText(marshallerRef) || StringUtils.hasText(unmarshallerRef)) {
			String extractPayload = element.getAttribute("extract-payload");
			if (StringUtils.hasText(extractPayload)) {
				this.logger.warn("Setting 'extract-payload' attribute has no effect when used with a marshalling Web Service Inbound Gateway.");
			}
		}
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);
		IntegrationNamespaceUtils.configureHeaderMapper(element, builder, parserContext, DefaultSoapHeaderMapper.class, null);
	}

}
