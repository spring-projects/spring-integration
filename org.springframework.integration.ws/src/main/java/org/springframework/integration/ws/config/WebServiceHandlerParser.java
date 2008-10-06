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

package org.springframework.integration.ws.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.adapter.config.AbstractRemotingOutboundGatewayParser;
import org.springframework.integration.ws.handler.MarshallingWebServiceHandler;
import org.springframework.integration.ws.handler.SimpleWebServiceHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;ws-handler/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class WebServiceHandlerParser extends AbstractRemotingOutboundGatewayParser {

	@Override
	protected Class<?> getGatewayClass(Element element) {
		return (StringUtils.hasText(element.getAttribute("marshaller"))) ?
				MarshallingWebServiceHandler.class : SimpleWebServiceHandler.class;
	}

	@Override
	protected String getInputChannelAttributeName() {
		return "input-channel";
	}

	@Override
	protected String parseUrl(Element element) {
		String uri = element.getAttribute("uri");
		Assert.hasText(uri, "The 'uri' attribute is required.");
		return uri;
	}

	@Override
	protected void postProcessGateway(BeanDefinitionBuilder builder, Element element) {
		String marshallerRef = element.getAttribute("marshaller");
		if (StringUtils.hasText(marshallerRef)) {
			builder.addConstructorArgReference(marshallerRef);
			String unmarshallerRef = element.getAttribute("unmarshaller");
			if (StringUtils.hasText(unmarshallerRef)) {
				builder.addConstructorArgReference(unmarshallerRef);
			}
		}
		else {
			String sourceExtractorRef = element.getAttribute("source-extractor");
			if (StringUtils.hasText(sourceExtractorRef)) {
				builder.addConstructorArgReference(sourceExtractorRef);
			}
			else {
				builder.addConstructorArgValue(null);
			}
		}
		String messageFactoryRef = element.getAttribute("message-factory");
		if (StringUtils.hasText(messageFactoryRef)) {
			builder.addConstructorArgReference(messageFactoryRef);
		}
		String requestCallbackRef = element.getAttribute("request-callback");
		if (StringUtils.hasText(requestCallbackRef)) {
			builder.addPropertyReference("requestCallback", requestCallbackRef);
		}
		String faultMessageResolverRef = element.getAttribute("fault-message-resolver");
		if (StringUtils.hasText(faultMessageResolverRef)) {
			builder.addPropertyReference("faultMessageResolver", faultMessageResolverRef);
		}
		String messageSenderRef = element.getAttribute("message-sender");
		String messageSenderListRef = element.getAttribute("message-senders");
		Assert.isTrue(!(StringUtils.hasText(messageSenderRef) && StringUtils.hasText(messageSenderListRef)),
				"Only one of message-sender or message-senders should be specified");
		if (StringUtils.hasText(messageSenderRef)) {
			builder.addPropertyReference("messageSender", messageSenderRef);
		}
		if (StringUtils.hasText(messageSenderListRef)) {
			builder.addPropertyReference("messageSenders", messageSenderListRef);
		}
	}

}
