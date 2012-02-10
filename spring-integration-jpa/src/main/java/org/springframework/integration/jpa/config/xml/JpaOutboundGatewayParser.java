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
package org.springframework.integration.jpa.config.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.outbound.JpaOutboundGateway;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * The Parser for JPA Outbound Gateway, the MessageHandler implementation is same as the 
 * outbound chanel adapter and hence we extend te class and setting the few additional 
 * attributes that we wish to in the MessageSource
 * 
 * @author Amol Nayak
 * @since 2.2
 *
 */
public class JpaOutboundGatewayParser extends AbstractConsumerEndpointParser  {
	
	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}
	
	protected BeanDefinitionBuilder parseHandler(Element gatewayElement, ParserContext parserContext) {
		
		final BeanDefinitionBuilder jpaExecutorBuilder = JpaParserUtils.getJpaExecutorBuilder(gatewayElement, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "entity-class");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "query");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "native-query");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "named-query");
		
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "persist-mode");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "parameter-source-factory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "use-payload-as-parameter-source");
				
		final BeanDefinition jpaExecutorBuilderBeanDefinition = jpaExecutorBuilder.getBeanDefinition();
		final String jpaExecutorBeanName = BeanDefinitionReaderUtils.generateBeanName(jpaExecutorBuilderBeanDefinition, parserContext.getRegistry());
		
		parserContext.registerBeanComponent(new BeanComponentDefinition(jpaExecutorBuilderBeanDefinition, jpaExecutorBeanName));
		
		final BeanDefinitionBuilder jpaOutboundGatewayBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(JpaOutboundGateway.class);
		
		jpaOutboundGatewayBuilder.addConstructorArgReference(jpaExecutorBeanName);
		
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaOutboundGatewayBuilder, gatewayElement, "gateway-type");
		
		final String replyChannel = gatewayElement.getAttribute("reply-channel");
		
		if (StringUtils.hasText(replyChannel)) {
			jpaOutboundGatewayBuilder.addPropertyReference("outputChannel", replyChannel);
		}
		
		return jpaOutboundGatewayBuilder;

	}
	
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}
	
}
