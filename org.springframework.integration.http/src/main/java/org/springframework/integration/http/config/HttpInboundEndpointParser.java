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

package org.springframework.integration.http.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

import org.w3c.dom.Element;

/**
 * Parser for the 'inbound-channel-adapter' and 'inbound-gateway' elements
 * of the 'http' namespace. The constructor's boolean value specifies whether
 * a reply is to be expected. This value should be 'false' for the
 * 'inbound-channel-adapter' and 'true' for the 'inbound-gateway'.  
 * 
 * @author Mark Fisher
 */
public class HttpInboundEndpointParser extends AbstractSingleBeanDefinitionParser {

	private final boolean expectReply;


	public HttpInboundEndpointParser(boolean expectReply) {
		this.expectReply = expectReply;
	}


	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.http.HttpInboundEndpoint";
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(id)) {
			id = element.getAttribute("name");
		}
		if (!StringUtils.hasText(id)) {
			parserContext.getReaderContext().error("The 'id' or 'name' is required.", element);
		}
		return id;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String inputChannelAttributeName = this.getInputChannelAttributeName();
		String inputChannelRef = element.getAttribute(inputChannelAttributeName);
		if (!StringUtils.hasText(inputChannelRef)) {
			parserContext.getReaderContext().error(
					"a '" + inputChannelAttributeName + "' reference is required", element);
		}
		builder.addPropertyReference("requestChannel", inputChannelRef);
		builder.addPropertyValue("expectReply", this.expectReply);
		if (this.expectReply) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-key");
		}
		else {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(
					builder, element, "send-timeout", "requestTimeout");
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "supported-methods");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "view");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-key");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-mapper");
	}

	private String getInputChannelAttributeName() {
		return this.expectReply ? "request-channel" : "channel";
	}

}
