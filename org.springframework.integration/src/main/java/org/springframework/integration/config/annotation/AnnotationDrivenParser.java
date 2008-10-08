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

package org.springframework.integration.config.annotation;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.MessageBusParser;

/**
 * Parser for the <em>annotation-driven</em> element of the integration
 * namespace. Registers the annotation-driven post-processors.
 * 
 * @author Mark Fisher
 */
public class AnnotationDrivenParser implements BeanDefinitionParser {

	private static final String PUBLISHER_ANNOTATION_POST_PROCESSOR_BEAN_NAME =
			"internal.PublisherAnnotationPostProcessor";

	private static final String MESSAGING_ANNOTATION_POST_PROCESSOR_BEAN_NAME =
			"internal.MessagingAnnotationPostProcessor";


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		this.registerPublisherPostProcessor(parserContext);
		this.registerMessagingAnnotationPostProcessor(parserContext);
		return null;
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
