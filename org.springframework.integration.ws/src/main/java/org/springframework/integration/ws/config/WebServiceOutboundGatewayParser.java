/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.adapter.config.AbstractRemotingOutboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-gateway/&gt; element in the 'ws' namespace. 
 * 
 * @author Mark Fisher
 * @author Jonas Partner
 */
public class WebServiceOutboundGatewayParser extends AbstractRemotingOutboundGatewayParser {

	private static final String BASE_PACKAGE = "org.springframework.integration.ws";


	@Override
	protected String getGatewayClassName(Element element) {
		String simpleClassName = (StringUtils.hasText(element.getAttribute("marshaller"))) ?
				"MarshallingWebServiceOutboundGateway" : "SimpleWebServiceOutboundGateway";
		return "org.springframework.integration.ws." + simpleClassName;
	}


	protected void buildDestinationProvider(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String uri = element.getAttribute("uri");
        String uriHeader = element.getAttribute("uri-header");
        String destinationProvider = element.getAttribute("destination-provider");

		if (StringUtils.hasText(destinationProvider)  && (StringUtils.hasText(uri) || StringUtils.hasText(uriHeader))) {
			parserContext.getReaderContext().error("The 'uri' and/or 'uri-header' are not allowed if setting destination-provider.", element);
		}

        if (!StringUtils.hasText(destinationProvider)  && !(StringUtils.hasText(uri) || StringUtils.hasText(uriHeader))) {
			parserContext.getReaderContext().error("At least one of 'uri' or 'uri-header' must be specified if not setting destination-provider.", element);
		}

        if (StringUtils.hasText(destinationProvider)) {
            builder.addConstructorArgReference(destinationProvider);
        }
        else if (StringUtils.hasText(uri)  && ! StringUtils.hasText(uriHeader)) {
            BeanDefinitionBuilder destinationProviderBuilder = BeanDefinitionBuilder.genericBeanDefinition(
            		BASE_PACKAGE + ".destination.FixedUriDestinationProvider");
            destinationProviderBuilder.getBeanDefinition().getConstructorArgumentValues().addIndexedArgumentValue(0, uri);
            builder.addConstructorArgValue(destinationProviderBuilder.getBeanDefinition());
        }
        else {
            BeanDefinitionBuilder destinationProviderBuilder = BeanDefinitionBuilder.genericBeanDefinition(
            		BASE_PACKAGE + ".destination.HeaderBasedDestinationProvider");
            destinationProviderBuilder.getBeanDefinition().getConstructorArgumentValues().addIndexedArgumentValue(0, uri);
            destinationProviderBuilder.getBeanDefinition().getConstructorArgumentValues().addIndexedArgumentValue(1, uriHeader);
            builder.addConstructorArgValue(destinationProviderBuilder.getBeanDefinition());
        }
	}

    @Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(this.getGatewayClassName(element));
		this.buildDestinationProvider(element, parserContext,builder);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-empty-responses");
		this.postProcessGateway(builder, element, parserContext);
		return builder;
	}

	@Override
	protected void postProcessGateway(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
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
		if (StringUtils.hasText(messageSenderRef) && StringUtils.hasText(messageSenderListRef)) {
			parserContext.getReaderContext().error(
					"Only one of message-sender or message-senders should be specified.", element);
		}
		if (StringUtils.hasText(messageSenderRef)) {
			builder.addPropertyReference("messageSender", messageSenderRef);
		}
		if (StringUtils.hasText(messageSenderListRef)) {
			builder.addPropertyReference("messageSenders", messageSenderListRef);
		}
	}

}
