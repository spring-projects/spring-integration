/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.http.config;

import java.util.List;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the 'inbound-channel-adapter' and 'inbound-gateway' elements
 * of the 'http' namespace. The constructor's boolean value specifies whether
 * a reply is to be expected. This value should be 'false' for the
 * 'inbound-channel-adapter' and 'true' for the 'inbound-gateway'.  
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class HttpInboundEndpointParser extends AbstractSingleBeanDefinitionParser {

	private final boolean expectReply;


	public HttpInboundEndpointParser(boolean expectReply) {
		this.expectReply = expectReply;
	}


	@Override
	protected String getBeanClassName(Element element) {
		return element.hasAttribute("view-name")
				? "org.springframework.integration.http.inbound.HttpRequestHandlingController"
				: "org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway";
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(id)) {
			id = element.getAttribute("name");
		} else {
			if (!element.hasAttribute(getInputChannelAttributeName())) {
				// the created channel will get the 'id', so the adapter's bean name includes a suffix
				id = id + ".adapter";
			}
		}
		if (!StringUtils.hasText(id)) {
			id = BeanDefinitionReaderUtils.generateBeanName(definition, parserContext.getRegistry());
		}

		return id;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addConstructorArgValue(this.expectReply);
		String inputChannelAttributeName = this.getInputChannelAttributeName();
		String inputChannelRef = element.getAttribute(inputChannelAttributeName);
		if (!StringUtils.hasText(inputChannelRef)) {
			if (this.expectReply) {
				parserContext.getReaderContext().error(
						"a '" + inputChannelAttributeName + "' reference is required", element);
			} else {
				inputChannelRef = createDirectChannel(element, parserContext);
			}
		}
		builder.addPropertyReference("requestChannel", inputChannelRef);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		
		
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "path");
		String payloadExpression = element.getAttribute("payload-expression");
		if (StringUtils.hasText(payloadExpression)){
			RootBeanDefinition expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(payloadExpression);
			builder.addPropertyValue("payloadExpression", expressionDef);
		}
		
		List<Element> headerElements = DomUtils.getChildElementsByTagName(element, "header");
		
		if (!CollectionUtils.isEmpty(headerElements)) {
			ManagedMap<String, Object> headerElementsMap = new ManagedMap<String, Object>();
			for (Element headerElement : headerElements) {
				String name = headerElement.getAttribute("name");
				String expression = headerElement.getAttribute("expression");
				if (StringUtils.hasText(expression)){
					RootBeanDefinition expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
					expressionDef.getConstructorArgumentValues().addGenericArgumentValue(expression);
					headerElementsMap.put(name, expressionDef);
				}			
			}
			builder.addPropertyValue("headerExpressions", headerElementsMap);
		}
		
		if (this.expectReply) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-key");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "convert-exceptions");
		}
		else {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(
					builder, element, "send-timeout", "requestTimeout");
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "supported-methods", "supportedMethodNames");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-payload-type");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "view-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "errors-key");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "error-code");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converters");
		
		
		//IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "header-mapper");
		String headerMapper = element.getAttribute("header-mapper");
		String mappedRequestHeaders = element.getAttribute("mapped-request-headers");
		String mappedResponseHeaders = element.getAttribute("mapped-response-headers");
		
		
		if (StringUtils.hasText(headerMapper)) {
			if (StringUtils.hasText(mappedRequestHeaders) || StringUtils.hasText(mappedResponseHeaders)) {
				parserContext.getReaderContext().error("Neither 'mappped-request-headers' or 'mapped-response-headers' " +
						"attributes are allowed when a 'header-mapper' has been specified.", parserContext.extractSource(element));
			}
			builder.addPropertyReference("headerMapper", headerMapper);
		}
		else {
			BeanDefinitionBuilder headerMapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(
									"org.springframework.integration.http.support.DefaultHttpHeaderMapper");
			headerMapperBuilder.setFactoryMethod("inboundMapper");
			
			IntegrationNamespaceUtils.setValueIfAttributeDefined(headerMapperBuilder, element, "mapped-request-headers", "inboundHeaderNames");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(headerMapperBuilder, element, "mapped-response-headers", "outboundHeaderNames");
			builder.addPropertyValue("headerMapper", headerMapperBuilder.getBeanDefinition());
		}
	}

	private String getInputChannelAttributeName() {
		return this.expectReply ? "request-channel" : "channel";
	}

	private String createDirectChannel(Element element, ParserContext parserContext) {
		String channelId = element.getAttribute("id");
		if (!StringUtils.hasText(channelId)) {
			parserContext.getReaderContext().error("The channel-adapter's 'id' attribute is required when no 'channel' "
					+ "reference has been provided, because that 'id' would be used for the created channel.", element);
		}
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class);
		BeanDefinitionHolder holder = new BeanDefinitionHolder(channelBuilder.getBeanDefinition(), channelId);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		return channelId;
	}
}
