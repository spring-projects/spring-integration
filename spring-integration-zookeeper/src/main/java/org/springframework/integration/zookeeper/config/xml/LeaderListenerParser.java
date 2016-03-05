/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.zookeeper.config.xml;

import java.util.UUID;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.DefaultLeaderEventPublisher;
import org.springframework.integration.zookeeper.leader.LeaderInitiator;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class LeaderListenerParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder candidateBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultCandidate.class);
		candidateBuilder.addConstructorArgValue(UUID.randomUUID().toString());
		candidateBuilder.addConstructorArgValue(element.getAttribute("role"));

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(LeaderInitiator.class);
		builder.addConstructorArgReference(element.getAttribute("client"));
		builder.addConstructorArgValue(candidateBuilder.getBeanDefinition());
		builder.addConstructorArgValue(element.getAttribute("path"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");

		BeanDefinitionBuilder publisherBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(DefaultLeaderEventPublisher.class);
		builder.addPropertyValue("leaderEventPublisher", publisherBuilder.getBeanDefinition());

		return builder.getBeanDefinition();
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

}
