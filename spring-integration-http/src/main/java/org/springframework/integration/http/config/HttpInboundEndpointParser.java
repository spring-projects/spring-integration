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

package org.springframework.integration.http.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.http.inbound.CrossOrigin;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the 'inbound-channel-adapter' and 'inbound-gateway' elements
 * of the 'http' namespace. The constructor's boolean value specifies whether
 * a reply is to be expected. This value should be 'false' for the
 * 'inbound-channel-adapter' and 'true' for the 'inbound-gateway'.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Biju Kunjummen
 * @author Artem Bilan
 */
public class HttpInboundEndpointParser extends AbstractSingleBeanDefinitionParser {

	private final boolean expectReply;

	public HttpInboundEndpointParser(boolean expectReply) {
		this.expectReply = expectReply;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return element.hasAttribute("view-name") || element.hasAttribute("view-expression")
				? HttpRequestHandlingController.class.getName()
				: HttpRequestHandlingMessagingGateway.class.getName();
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);

		if (!this.expectReply && !element.hasAttribute("channel")) {
			// the created channel will get the 'id', so the adapter's bean name includes a suffix
			id = id + ".adapter";
		}
		if (!StringUtils.hasText(id)) {
			id = BeanDefinitionReaderUtils.generateBeanName(definition, parserContext.getRegistry());
		}

		return id;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addConstructorArgValue(this.expectReply);
		parseInputChannel(element, parserContext, builder);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");

		BeanDefinition payloadExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("payload-expression", element);
		if (payloadExpressionDef != null) {
			builder.addPropertyValue("payloadExpression", payloadExpressionDef);
		}

		parseHeaders(element, builder);

		if (this.expectReply) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-key");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "convert-exceptions");
		}
		else {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout", "requestTimeout");
		}

		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("view-name",
						"view-expression", parserContext, element, false);
		if (expressionDef != null) {
			builder.addPropertyValue("viewExpression", expressionDef);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "errors-key");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "error-code");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converters");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "merge-with-default-converters");


		parseHeaderMapper(element, parserContext, builder);

		BeanDefinition requestMappingDef = createRequestMapping(element);
		builder.addPropertyValue("requestMapping", requestMappingDef);


		parseCrossOrigin(element, builder);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				"request-payload-type", "requestPayloadTypeClass");

		BeanDefinition statusCodeExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("status-code-expression", element);
		if (statusCodeExpressionDef == null) {
			statusCodeExpressionDef = IntegrationNamespaceUtils
					.createExpressionDefIfAttributeDefined("reply-timeout-status-code-expression", element);
		}
		if (statusCodeExpressionDef != null) {
			builder.addPropertyValue("statusCodeExpression", statusCodeExpressionDef);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.AUTO_STARTUP);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.PHASE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "validator");
	}

	private void parseInputChannel(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String inputChannelAttributeName = this.getInputChannelAttributeName();
		String inputChannelRef = element.getAttribute(inputChannelAttributeName);
		if (!StringUtils.hasText(inputChannelRef)) {
			if (this.expectReply) {
				parserContext.getReaderContext().error(
						"a '" + inputChannelAttributeName + "' reference is required", element);
			}
			else {
				inputChannelRef = IntegrationNamespaceUtils.createDirectChannel(element, parserContext);
			}
		}
		builder.addPropertyReference("requestChannel", inputChannelRef);
	}

	private String getInputChannelAttributeName() {
		return this.expectReply ? "request-channel" : "channel";
	}

	private void parseHeaders(Element element, BeanDefinitionBuilder builder) {
		List<Element> headerElements = DomUtils.getChildElementsByTagName(element, "header");

		if (!CollectionUtils.isEmpty(headerElements)) {
			ManagedMap<String, Object> headerElementsMap = new ManagedMap<>();
			for (Element headerElement : headerElements) {
				String name = headerElement.getAttribute(NAME_ATTRIBUTE);
				BeanDefinition headerExpressionDef =
						IntegrationNamespaceUtils
								.createExpressionDefIfAttributeDefined(IntegrationNamespaceUtils.EXPRESSION_ATTRIBUTE,
										headerElement);
				if (headerExpressionDef != null) {
					headerElementsMap.put(name, headerExpressionDef);
				}
			}
			builder.addPropertyValue("headerExpressions", headerElementsMap);
		}
	}

	private void parseHeaderMapper(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String mappedRequestHeaders = element.getAttribute("mapped-request-headers");
		String mappedResponseHeaders = element.getAttribute("mapped-response-headers");

		boolean hasMappedRequestHeaders = StringUtils.hasText(mappedRequestHeaders);
		boolean hasMappedResponseHeaders = StringUtils.hasText(mappedResponseHeaders);

		String headerMapper = element.getAttribute("header-mapper");
		if (StringUtils.hasText(headerMapper)) {
			if (hasMappedRequestHeaders || hasMappedResponseHeaders) {
				parserContext.getReaderContext()
						.error("Neither 'mapped-request-headers' or 'mapped-response-headers' " +
										"attributes are allowed when a 'header-mapper' has been specified.",
								parserContext.extractSource(element));
			}
			builder.addPropertyReference("headerMapper", headerMapper);
		}
		else {
			BeanDefinitionBuilder headerMapperBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(DefaultHttpHeaderMapper.class);
			headerMapperBuilder.setFactoryMethod("inboundMapper");

			if (hasMappedRequestHeaders) {
				headerMapperBuilder.addPropertyValue("inboundHeaderNames", mappedRequestHeaders);
			}
			if (hasMappedResponseHeaders) {
				headerMapperBuilder.addPropertyValue("outboundHeaderNames", mappedResponseHeaders);
			}

			builder.addPropertyValue("headerMapper", headerMapperBuilder.getBeanDefinition());
		}
	}

	private void parseCrossOrigin(Element element, BeanDefinitionBuilder builder) {
		Element crossOriginElement = DomUtils.getChildElementByTagName(element, "cross-origin");
		if (crossOriginElement != null) {
			BeanDefinitionBuilder crossOriginBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(CrossOrigin.class);
			String[] attributes = { "origin", "allowed-headers", "exposed-headers", "max-age", "method" };
			for (String crossOriginAttribute : attributes) {
				IntegrationNamespaceUtils.setValueIfAttributeDefined(crossOriginBuilder, crossOriginElement,
						crossOriginAttribute);
			}
			IntegrationNamespaceUtils.setValueIfAttributeDefined(crossOriginBuilder, crossOriginElement,
					"allow-credentials", true);
			builder.addPropertyValue("crossOrigin", crossOriginBuilder.getBeanDefinition());
		}
	}

	private BeanDefinition createRequestMapping(Element element) {
		BeanDefinitionBuilder requestMappingDefBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(RequestMapping.class);

		String methods = element.getAttribute("supported-methods");
		if (StringUtils.hasText(methods)) {
			requestMappingDefBuilder.addPropertyValue("methods", methods.toUpperCase());
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(requestMappingDefBuilder, element, "path", "pathPatterns");

		Element requestMappingElement = DomUtils.getChildElementByTagName(element, "request-mapping");

		if (requestMappingElement != null) {
			for (String requestMappingAttribute : new String[]{ "params", "headers", "consumes", "produces" }) {
				IntegrationNamespaceUtils.setValueIfAttributeDefined(requestMappingDefBuilder, requestMappingElement,
						requestMappingAttribute);
			}
		}

		return requestMappingDefBuilder.getRawBeanDefinition();
	}

}
