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

package org.springframework.integration.ws.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractOutboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ws.DefaultSoapHeaderMapper;
import org.springframework.integration.ws.MarshallingWebServiceOutboundGateway;
import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;outbound-gateway/&gt; element in the 'ws' namespace.
 *
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class WebServiceOutboundGatewayParser extends AbstractOutboundGatewayParser {

	@Override
	protected String getGatewayClassName(Element element) {
		return ((StringUtils.hasText(element.getAttribute("marshaller"))) ?
				MarshallingWebServiceOutboundGateway.class : SimpleWebServiceOutboundGateway.class).getName();
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(getGatewayClassName(element));
		String uri = element.getAttribute("uri");
		String destinationProvider = element.getAttribute("destination-provider");
		List<Element> uriVariableElements = DomUtils.getChildElementsByTagName(element, "uri-variable");
		if (StringUtils.hasText(destinationProvider) == StringUtils.hasText(uri)) {
			parserContext.getReaderContext().error(
					"Exactly one of 'uri' or 'destination-provider' is required.", element);
		}
		if (StringUtils.hasText(destinationProvider)) {
			if (!CollectionUtils.isEmpty(uriVariableElements)) {
				parserContext.getReaderContext().error("No 'uri-variable' sub-elements are allowed when "
						+ "a 'destination-provider' reference has been provided.", element);
			}
			builder.addConstructorArgReference(destinationProvider);
		}
		else {
			builder.addConstructorArgValue(uri);
			if (!CollectionUtils.isEmpty(uriVariableElements)) {
				ManagedMap<String, Object> uriVariableExpressions = new ManagedMap<>();
				for (Element uriVariableElement : uriVariableElements) {
					String name = uriVariableElement.getAttribute("name");
					String expression = uriVariableElement.getAttribute("expression");
					BeanDefinitionBuilder factoryBeanBuilder =
							BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
					factoryBeanBuilder.addConstructorArgValue(expression);
					uriVariableExpressions.put(name, factoryBeanBuilder.getBeanDefinition());
				}
				builder.addPropertyValue("uriVariableExpressions", uriVariableExpressions);
			}
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-empty-responses");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "encode-uri");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "encoding-mode");
		postProcessGateway(builder, element, parserContext);

		IntegrationNamespaceUtils.configureHeaderMapper(element, builder, parserContext,
				DefaultSoapHeaderMapper.class, null);

		return builder;
	}

	@Override // NOSONAR
	protected void postProcessGateway(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		parseMarshallerAttribute(builder, element, parserContext);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-callback");

		String webServiceTemplateRef = element.getAttribute("web-service-template");

		if (StringUtils.hasText(webServiceTemplateRef)) {
			builder.addPropertyReference("webServiceTemplate", webServiceTemplateRef);
			return;
		}

		String messageFactoryRef = element.getAttribute("message-factory");
		if (StringUtils.hasText(messageFactoryRef)) {
			builder.addConstructorArgReference(messageFactoryRef);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "fault-message-resolver");

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
		String interceptorRef = element.getAttribute("interceptor");
		String interceptorListRef = element.getAttribute("interceptors");
		if (StringUtils.hasText(interceptorRef) && StringUtils.hasText(interceptorListRef)) {
			parserContext.getReaderContext().error(
					"Only one of interceptor or interceptors should be specified.", element);
		}
		if (StringUtils.hasText(interceptorRef)) {
			builder.addPropertyReference("interceptors", interceptorRef);
		}
		if (StringUtils.hasText(interceptorListRef)) {
			builder.addPropertyReference("interceptors", interceptorListRef);
		}
	}

	private void parseMarshallerAttribute(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String marshallerRef = element.getAttribute("marshaller");
		String unmarshallerRef = element.getAttribute("unmarshaller");
		if (StringUtils.hasText(marshallerRef)) {
			builder.addConstructorArgReference(marshallerRef);
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

		if (StringUtils.hasText(marshallerRef) || StringUtils.hasText(unmarshallerRef)) {
			String extractPayload = element.getAttribute("extract-payload");
			if (StringUtils.hasText(extractPayload)) {
				parserContext.getReaderContext()
						.warning("Setting 'extract-payload' attribute has no effect when used with " +
								"a marshalling Web Service Outbound Gateway.", element);
			}
		}
		else {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		}
	}

}
