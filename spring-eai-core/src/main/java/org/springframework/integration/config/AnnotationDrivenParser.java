/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * @author Mark Fisher
 */
public class AnnotationDrivenParser implements BeanDefinitionParser {

	private static final String PUBLISHER_ANNOTATION_POST_PROCESSOR_BEAN_NAME =
			"internal.PublisherAnnotationPostProcessor";

	private static final String SUBSCRIBER_ANNOTATION_POST_PROCESSOR_BEAN_NAME = 
			"internal.SubscriberAnnotationPostProcessor";


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		this.createPublisherPostProcessor(parserContext);
		this.createSubscriberPostProcessor(parserContext);
		return null;
	}

	private void createPublisherPostProcessor(ParserContext parserContext) {
		BeanDefinition bd = new RootBeanDefinition(PublisherAnnotationPostProcessor.class);
		bd.getPropertyValues().addPropertyValue("channelMapping",
				new RuntimeBeanReference(MessageBusParser.MESSAGE_BUS_BEAN_NAME));
		BeanComponentDefinition bcd = new BeanComponentDefinition(
				bd, PUBLISHER_ANNOTATION_POST_PROCESSOR_BEAN_NAME);
		parserContext.registerBeanComponent(bcd);
	}

	private void createSubscriberPostProcessor(ParserContext parserContext) {
		BeanDefinition bd = new RootBeanDefinition(SubscriberAnnotationPostProcessor.class);
		bd.getPropertyValues().addPropertyValue("messageBus",
				new RuntimeBeanReference(MessageBusParser.MESSAGE_BUS_BEAN_NAME));
		BeanComponentDefinition bcd = new BeanComponentDefinition(
				bd, SUBSCRIBER_ANNOTATION_POST_PROCESSOR_BEAN_NAME);
		parserContext.registerBeanComponent(bcd);
	}

}
