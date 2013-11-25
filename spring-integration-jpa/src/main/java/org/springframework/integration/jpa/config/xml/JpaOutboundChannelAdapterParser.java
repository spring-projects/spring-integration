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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.outbound.JpaOutboundGatewayFactoryBean;
import org.springframework.util.xml.DomUtils;

/**
 * The parser for JPA outbound channel adapter
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 *
 * @since 2.2
 *
 */
public class JpaOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {

		final BeanDefinitionBuilder jpaOutboundChannelAdapterBuilder = BeanDefinitionBuilder.genericBeanDefinition(JpaOutboundGatewayFactoryBean.class);
		final BeanDefinitionBuilder jpaExecutorBuilder = JpaParserUtils.getJpaExecutorBuilder(element, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "persist-mode");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "flush");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "flush-size");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "clear-on-flush");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "parameter-source-factory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "use-payload-as-parameter-source");

		final BeanDefinition jpaExecutorBuilderBeanDefinition = jpaExecutorBuilder.getBeanDefinition();
		final String channelAdapterId = this.resolveId(element, jpaOutboundChannelAdapterBuilder.getRawBeanDefinition(), parserContext);
		final String jpaExecutorBeanName = channelAdapterId + ".jpaExecutor";

		parserContext.registerBeanComponent(new BeanComponentDefinition(jpaExecutorBuilderBeanDefinition, jpaExecutorBeanName));

		jpaOutboundChannelAdapterBuilder.addConstructorArgReference(jpaExecutorBeanName);
		jpaOutboundChannelAdapterBuilder.addPropertyValue("producesReply", Boolean.FALSE);

		final Element transactionalElement = DomUtils.getChildElementByTagName(element, "transactional");

		if(transactionalElement != null) {
			BeanDefinition txAdviceDefinition = IntegrationNamespaceUtils.configureTransactionAttributes(transactionalElement);
			ManagedList<BeanDefinition> adviceChain = new ManagedList<BeanDefinition>();
			adviceChain.add(txAdviceDefinition);
			jpaOutboundChannelAdapterBuilder.addPropertyValue("txAdviceChain", adviceChain);
		}

		return jpaOutboundChannelAdapterBuilder.getBeanDefinition();

	}

	@Override
	protected boolean isUsingReplyProducer() {
		return true;
	}

}
