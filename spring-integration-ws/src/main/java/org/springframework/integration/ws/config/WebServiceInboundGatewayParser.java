/*
 * Copyright 2002-2011 the original author or authors.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.integration.ws.config;

import org.w3c.dom.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class WebServiceInboundGatewayParser extends AbstractInboundGatewayParser {
	protected final Log logger = LogFactory.getLog(getClass());
	@Override
	protected String getBeanClassName(Element element) {
		String simpleClassName = (StringUtils.hasText(element.getAttribute("marshaller"))) ?
				"MarshallingWebServiceInboundGateway" : "SimpleWebServiceInboundGateway";
		return "org.springframework.integration.ws." + simpleClassName;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !(attributeName.endsWith("marshaller")) && super.isEligibleAttribute(attributeName);
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
			if (StringUtils.hasText(unmarshallerRef)){
				throw new IllegalArgumentException("Defining 'unmarshaller' without 'marshaller' is not allowed");
			}
		}
		
		if (StringUtils.hasText(marshallerRef) || StringUtils.hasText(unmarshallerRef)){
			String extractPayload = element.getAttribute("extract-payload");
			if (StringUtils.hasText(extractPayload)){
				logger.warn("Setting 'extract-payload' attribute ihas no effect when used with MarshallingWebServiceInboundGateway");
			}
		}
		
		String headerMapperRef = element.getAttribute("header-mapper");
		if (StringUtils.hasText(headerMapperRef)) {
			Assert.isTrue(!StringUtils.hasText(marshallerRef),
					"The 'header-mapper' attribute cannot be used when a 'marshaller' is provided.");
			builder.addPropertyReference("headerMapper", headerMapperRef);
		}
	}

}
