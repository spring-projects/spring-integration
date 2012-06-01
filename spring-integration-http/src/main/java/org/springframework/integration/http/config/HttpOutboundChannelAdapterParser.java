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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'outbound-channel-adapter' element of the http namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class HttpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(HttpRequestExecutingMessageHandler.class);
		builder.addPropertyValue("expectReply", false);
		HttpAdapterParsingUtils.configureUrlConstructorArg(element, parserContext, builder);

		String httpMethod = element.getAttribute("http-method");
		String httpMethodExpression = element.getAttribute("http-method-expression");

		boolean hasHttpMethod = StringUtils.hasText(httpMethod);
		boolean hasHttpMethodExpression = StringUtils.hasText(httpMethodExpression);

		if (hasHttpMethod && hasHttpMethodExpression){
			parserContext.getReaderContext().error("The 'http-method' and 'http-method-expression' are mutually exclusive. " +
					"You can only have one or the other", element);
		}

		RootBeanDefinition expressionDef = null;
		if (hasHttpMethod) {
			expressionDef = new RootBeanDefinition(LiteralExpression.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(httpMethod);
		}
		else if (hasHttpMethodExpression){
			expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(httpMethodExpression);
		}
		if (expressionDef != null){
			builder.addPropertyValue("httpMethodExpression", expressionDef);
		}

		String restTemplate = element.getAttribute("rest-template");
		if (StringUtils.hasText(restTemplate)) {
			HttpAdapterParsingUtils.verifyNoRestTemplateAttributes(element, parserContext);
			builder.addConstructorArgReference(restTemplate);
		}
		else {
			for (String referenceAttributeName : HttpAdapterParsingUtils.REST_TEMPLATE_REFERENCE_ATTRIBUTES) {
				IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, referenceAttributeName);
			}
		}

		String headerMapper = element.getAttribute("header-mapper");
		String mappedRequestHeaders = element.getAttribute("mapped-request-headers");
		if (StringUtils.hasText(headerMapper)) {
			if (StringUtils.hasText(mappedRequestHeaders)) {
				parserContext.getReaderContext().error("The 'mappped-request-headers' attribute is not " +
						"allowed when a 'header-mapper' has been specified.", parserContext.extractSource(element));
				return null;
			}
			builder.addPropertyReference("headerMapper", headerMapper);
		}
		else if (StringUtils.hasText(mappedRequestHeaders)) {
			BeanDefinitionBuilder headerMapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.integration.http.support.DefaultHttpHeaderMapper");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(headerMapperBuilder, element, "mapped-request-headers", "outboundHeaderNames");
			builder.addPropertyValue("headerMapper", headerMapperBuilder.getBeanDefinition());
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expected-response-type");
		HttpAdapterParsingUtils.configureUriVariableExpressions(builder, element);
		return builder.getBeanDefinition();
	}

}
