/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.adapter.config.AbstractRemotingGatewayParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * 
 * @author Iwein Fuld
 *
 */
public class WebServiceInboundGatewayParser extends AbstractRemotingGatewayParser {

	@Override
	protected String getBeanClassName(Element element) {
		String simpleClassName = (StringUtils.hasText(element.getAttribute("marshaller"))) ?
				"MarshallingWebServiceInboundGateway" : "SimpleWebServiceInboundGateway";
		return "org.springframework.integration.ws." + simpleClassName;
	}
	
	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		String marshallerRef = element.getAttribute("marshaller");
		if (StringUtils.hasText(marshallerRef)) {
			builder.addPropertyReference("marshaller",marshallerRef);
			String unmarshallerRef = element.getAttribute("unmarshaller");
			if (StringUtils.hasText(unmarshallerRef)) {
				builder.addPropertyReference("unmarshaller",unmarshallerRef);
			}
		}
	}
}
