/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.MessageBusAwareBeanPostProcessor;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.integration.config.annotation.PublisherAnnotationPostProcessor;
import org.springframework.util.Assert;

/**
 * Parser for the &lt;message-bus&gt; element of the integration namespace.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class MessageBusParser extends AbstractSimpleBeanDefinitionParser {

	public static final String MESSAGE_BUS_BEAN_NAME = "internal.MessageBus";

	public static final String MESSAGE_BUS_AWARE_POST_PROCESSOR_BEAN_NAME =
			"internal.MessageBusAwareBeanPostProcessor";

	private static final String PUBLISHER_ANNOTATION_POST_PROCESSOR_BEAN_NAME =
			"internal.PublisherAnnotationPostProcessor";

	private static final String MESSAGING_ANNOTATION_POST_PROCESSOR_BEAN_NAME =
			"internal.MessagingAnnotationPostProcessor";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		Assert.state(!parserContext.getRegistry().containsBeanDefinition(MESSAGE_BUS_BEAN_NAME),
				"Only one instance of '" + MessageBus.class.getSimpleName() + "' is allowed per ApplicationContext.");
		return MESSAGE_BUS_BEAN_NAME;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return DefaultMessageBus.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !"task-scheduler".equals(attributeName) &&
				!"enable-annotations".equals(attributeName) &&
				super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-scheduler");
		this.processChildElements(builder, element);
	}

	@SuppressWarnings("unchecked")
	private void processChildElements(BeanDefinitionBuilder builder, Element element) {
		NodeList childNodes = element.getChildNodes();
		ManagedList interceptors = new ManagedList();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if ("interceptor".equals(child.getLocalName())) {
					interceptors.add(new RuntimeBeanReference(((Element)child).getAttribute("ref")));
				}
			}
		}
		if (interceptors.size() > 0) {
			builder.addPropertyValue("interceptors", interceptors);
		}
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);
		this.addPostProcessors(element, parserContext);
	}

	/**
	 * Adds extra post-processors to the context, to inject the objects configured by the MessageBus
	 */
	private void addPostProcessors(Element element, ParserContext parserContext) {
		this.registerMessageBusAwarePostProcessor(parserContext);
		if ("true".equals(element.getAttribute("enable-annotations").toLowerCase())) {
			this.registerPublisherPostProcessor(parserContext);
			this.registerMessagingAnnotationPostProcessor(parserContext);
		}
	}

	private void registerMessageBusAwarePostProcessor(ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MessageBusAwareBeanPostProcessor.class);
		builder.addConstructorArgReference(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		parserContext.getRegistry().registerBeanDefinition(MESSAGE_BUS_AWARE_POST_PROCESSOR_BEAN_NAME, builder.getBeanDefinition());
	}

	private void registerPublisherPostProcessor(ParserContext parserContext) {
		BeanDefinition bd = new RootBeanDefinition(PublisherAnnotationPostProcessor.class);
		bd.getPropertyValues().addPropertyValue("channelRegistry",
				new RuntimeBeanReference(MessageBusParser.MESSAGE_BUS_BEAN_NAME));
		BeanComponentDefinition bcd = new BeanComponentDefinition(
				bd, PUBLISHER_ANNOTATION_POST_PROCESSOR_BEAN_NAME);
		parserContext.registerBeanComponent(bcd);
	}

	private void registerMessagingAnnotationPostProcessor(ParserContext parserContext) {
		BeanDefinition bd = new RootBeanDefinition(MessagingAnnotationPostProcessor.class);
		BeanComponentDefinition bcd = new BeanComponentDefinition(
				bd, MESSAGING_ANNOTATION_POST_PROCESSOR_BEAN_NAME);
		parserContext.registerBeanComponent(bcd);
	}

}
