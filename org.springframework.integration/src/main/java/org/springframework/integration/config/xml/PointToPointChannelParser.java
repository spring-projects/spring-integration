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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;channel&gt; element.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class PointToPointChannelParser extends AbstractChannelParser {

	private static final String CHANNEL_PACKAGE = IntegrationNamespaceUtils.BASE_PACKAGE + ".channel";

	private static final String DISPATCHER_PACKAGE = IntegrationNamespaceUtils.BASE_PACKAGE + ".dispatcher";

	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = null;
		Element queueElement = null;

		// configure a queue-based channel if any queue sub-element is defined
		if ((queueElement = DomUtils.getChildElementByTagName(element, "queue")) != null) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(CHANNEL_PACKAGE + ".QueueChannel");
			boolean hasCapacity = this.parseQueueCapacity(builder, queueElement);
			boolean hasQueueRef = this.parseQueueRef(builder, queueElement);
			if (hasCapacity && hasQueueRef) {
				parserContext.getReaderContext().error("The 'capacity' attribute is not allowed" +
						" when providing a 'ref' to a custom queue.", element);
			}
		}
		else if ((queueElement = DomUtils.getChildElementByTagName(element, "priority-queue")) != null) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(CHANNEL_PACKAGE + ".PriorityChannel");
			this.parseQueueCapacity(builder, queueElement);
			String comparatorRef = queueElement.getAttribute("comparator");
			if (StringUtils.hasText(comparatorRef)) {
				builder.addConstructorArgReference(comparatorRef);
			}
		}
		else if ((queueElement = DomUtils.getChildElementByTagName(element, "rendezvous-queue")) != null) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(CHANNEL_PACKAGE + ".RendezvousChannel");
		}

		// verify that the 'task-executor' is not provided if a queue sub-element exists
		String taskExecutor = element.getAttribute("task-executor");
		if (queueElement != null && StringUtils.hasText(taskExecutor)) {
			parserContext.getReaderContext().error("The 'task-executor' attribute " +
					"and any queue sub-element are mutually exclusive.", element);
			return null;
		}

		if (builder == null) {
			// configure either an ExecutorChannel or DirectChannel based on existence of 'task-executor'
			if (StringUtils.hasText(taskExecutor)) {
				builder = BeanDefinitionBuilder.genericBeanDefinition(CHANNEL_PACKAGE + ".ExecutorChannel");
				builder.addConstructorArgReference(taskExecutor);
			}
			else {
				builder = BeanDefinitionBuilder.genericBeanDefinition(CHANNEL_PACKAGE + ".DirectChannel");
			}
			parseDispatcher(element.getAttribute("dispatcher"), builder, parserContext);
		}
		return builder;
	}


	private void parseDispatcher(String dispatcherAttribute, BeanDefinitionBuilder builder, ParserContext parserContext) {
		if (dispatcherAttribute != null) {
			if (dispatcherAttribute.equals("failover")) {
				BeanDefinitionBuilder dispatcherBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(DISPATCHER_PACKAGE + ".FailOverDispatcher");
				dispatcherBuilder.setRole(BeanDefinition.ROLE_SUPPORT);
				builder.addConstructorArgReference(BeanDefinitionReaderUtils.registerWithGeneratedName(dispatcherBuilder
						.getBeanDefinition(), parserContext.getRegistry()));
			}
		}
		// rely on the default for round-robin
	}

	private boolean parseQueueCapacity(BeanDefinitionBuilder builder, Element queueElement) {
		String capacity = queueElement.getAttribute("capacity");
		if (StringUtils.hasText(capacity)) {
			builder.addConstructorArgValue(capacity);
			return true;
		}
		return false;
	}

	private boolean parseQueueRef(BeanDefinitionBuilder builder, Element queueElement) {
		String queueRef = queueElement.getAttribute("ref");
		if (StringUtils.hasText(queueRef)){
			builder.addConstructorArgReference(queueRef);
			return true;
		}
		return false;
	}

}
