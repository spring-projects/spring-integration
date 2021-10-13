/*
 * Copyright 2002-2021 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'outbound-channel-adapter' element of the http namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Shiliang Li
 *
 * @since 2.0
 */
public class HttpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = getBuilder(element, parserContext);

		builder.addPropertyValue("expectReply", false);
		HttpAdapterParsingUtils.configureUrlConstructorArg(element, parserContext, builder);
		HttpAdapterParsingUtils.setHttpMethodOrExpression(element, parserContext, builder);

		String headerMapper = element.getAttribute("header-mapper");
		String mappedRequestHeaders = element.getAttribute("mapped-request-headers");
		if (StringUtils.hasText(headerMapper)) {
			if (StringUtils.hasText(mappedRequestHeaders)) {
				parserContext.getReaderContext().error("The 'mapped-request-headers' attribute is not " +
						"allowed when a 'header-mapper' has been specified.", parserContext.extractSource(element));
				return null;
			}
			builder.addPropertyReference("headerMapper", headerMapper);
		}
		else if (StringUtils.hasText(mappedRequestHeaders)) {
			BeanDefinitionBuilder headerMapperBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(DefaultHttpHeaderMapper.class);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(headerMapperBuilder, element,
					"mapped-request-headers", "outboundHeaderNames");
			builder.addPropertyValue("headerMapper", headerMapperBuilder.getBeanDefinition());
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "trusted-spel");
		HttpAdapterParsingUtils.setExpectedResponseOrExpression(element, parserContext, builder);
		HttpAdapterParsingUtils.configureUriVariableExpressions(builder, parserContext, element);
		return builder.getBeanDefinition();
	}

	protected BeanDefinitionBuilder getBuilder(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(HttpRequestExecutingMessageHandler.class);

		String restTemplateRef = element.getAttribute("rest-template");

		if (StringUtils.hasText(restTemplateRef)) {
			HttpAdapterParsingUtils.verifyNoRestTemplateAttributes(element, parserContext);
			builder.getBeanDefinition()
					.getConstructorArgumentValues()
					.addIndexedArgumentValue(1, new RuntimeBeanReference(restTemplateRef));
		}
		else {
			for (String referenceAttributeName : HttpAdapterParsingUtils.SYNC_REST_TEMPLATE_REFERENCE_ATTRIBUTES) {
				IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, referenceAttributeName);
			}
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "encoding-mode");
		}
		return builder;
	}

}
