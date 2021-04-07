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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;channel&gt; element.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Manuel Jordan
 */
public class PointToPointChannelParser extends AbstractChannelParser {

	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = null;
		Element queueElement;
		String fixedSubscriberChannel = element.getAttribute("fixed-subscriber");
		boolean isFixedSubscriber = "true".equalsIgnoreCase(fixedSubscriberChannel.trim());

		// configure a queue-based channel if any queue sub-element is defined
		String channel = element.getAttribute(ID_ATTRIBUTE);
		if ((queueElement = DomUtils.getChildElementByTagName(element, "queue")) != null) { // NOSONAR inner assignment
			builder = queue(element, parserContext, queueElement, channel);
		}
		else if ((queueElement = DomUtils.getChildElementByTagName(element, "priority-queue")) != null) { // NOSONAR
			builder = priorityQueue(element, parserContext, queueElement, channel);

		}
		else if ((queueElement = DomUtils.getChildElementByTagName(element, "rendezvous-queue")) != null) { // NOSONAR
			builder = BeanDefinitionBuilder.genericBeanDefinition(RendezvousChannel.class);
		}

		Element dispatcherElement = DomUtils.getChildElementByTagName(element, "dispatcher");

		// verify that a dispatcher is not provided if a queue sub-element exists
		if (queueElement != null && dispatcherElement != null) {
			parserContext.getReaderContext().error(
					"The 'dispatcher' sub-element and any queue sub-element are mutually exclusive.", element);
			return null;
		}

		if (queueElement != null) {
			if (isFixedSubscriber) {
				parserContext.getReaderContext().error(
						"The 'fixed-subscriber' attribute is not allowed when a <queue/> child element is present.",
						element);
			}
			return builder;
		}

		if (dispatcherElement == null) {
			// configure the default DirectChannel with a RoundRobinLoadBalancingStrategy
			builder = BeanDefinitionBuilder.genericBeanDefinition(isFixedSubscriber ? FixedSubscriberChannel.class
					: DirectChannel.class);
		}
		else {
			builder = dispatcher(element, parserContext, isFixedSubscriber, dispatcherElement);
		}
		return builder;
	}

	private BeanDefinitionBuilder queue(Element element, ParserContext parserContext, Element queueElement,
			String channel) {

		BeanDefinitionBuilder builder;
		builder = BeanDefinitionBuilder.genericBeanDefinition(QueueChannel.class);
		boolean hasStoreRef = this.parseStoreRef(builder, queueElement, channel, false);
		boolean hasQueueRef = this.parseQueueRef(builder, queueElement);
		if (!hasStoreRef || !hasQueueRef) {
			boolean hasCapacity = this.parseQueueCapacity(builder, queueElement);
			if (hasCapacity && hasQueueRef) {
				parserContext.getReaderContext().error(
						"The 'capacity' attribute is not allowed when providing a 'ref' to a custom queue.",
						element);
			}
			if (hasCapacity && hasStoreRef) {
				parserContext.getReaderContext().error(
						"The 'capacity' attribute is not allowed" +
								" when providing a 'message-store' to a custom MessageGroupStore.",
						element);
			}
		}
		if (hasStoreRef && hasQueueRef) {
			parserContext.getReaderContext().error(
					"The 'message-store' attribute is not allowed when providing a 'ref' to a custom queue.",
					element);
		}
		return builder;
	}

	private BeanDefinitionBuilder priorityQueue(Element element, ParserContext parserContext, Element queueElement,
			String channel) {

		BeanDefinitionBuilder builder;
		builder = BeanDefinitionBuilder.genericBeanDefinition(PriorityChannel.class);
		boolean hasCapacity = this.parseQueueCapacity(builder, queueElement);
		String comparatorRef = queueElement.getAttribute("comparator");
		if (StringUtils.hasText(comparatorRef)) {
			builder.addConstructorArgReference(comparatorRef);
		}
		if (parseStoreRef(builder, queueElement, channel, true)) {
			if (StringUtils.hasText(comparatorRef)) {
				parserContext.getReaderContext().error(
						"The 'message-store' attribute is not allowed" +
								" when providing a 'comparator' to a priority queue.",
						element);
			}
			if (hasCapacity) {
				parserContext.getReaderContext().error("The 'capacity' attribute is not allowed"
						+ " when providing a 'message-store' to a custom MessageGroupStore.", element);
			}
		}
		return builder;
	}

	private BeanDefinitionBuilder dispatcher(Element element, ParserContext parserContext, boolean isFixedSubscriber,
			Element dispatcherElement) {

		BeanDefinitionBuilder builder;
		if (isFixedSubscriber) {
			parserContext.getReaderContext().error(
					"The 'fixed-subscriber' attribute is not allowed" +
							" when a <dispatcher/> child element is present.",
					element);
		}
		// configure either an ExecutorChannel or DirectChannel based on existence of 'task-executor'
		String taskExecutor = dispatcherElement.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutor)) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(ExecutorChannel.class);
			builder.addConstructorArgReference(taskExecutor);
		}
		else {
			builder = BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class);
		}
		// unless the 'load-balancer' attribute is explicitly set to 'none'
		// or 'load-balancer-ref' is explicitly configured,
		// configure the default RoundRobinLoadBalancingStrategy
		String loadBalancer = dispatcherElement.getAttribute("load-balancer");
		String loadBalancerRef = dispatcherElement.getAttribute("load-balancer-ref");
		if (StringUtils.hasText(loadBalancer) && StringUtils.hasText(loadBalancerRef)) {
			parserContext.getReaderContext().error("'load-balancer' and 'load-balancer-ref' are mutually exclusive",
					element);
		}
		if (StringUtils.hasText(loadBalancerRef)) {
			builder.addConstructorArgReference(loadBalancerRef);
		}
		else {
			if ("none".equals(loadBalancer)) {
				builder.addConstructorArgValue(null);
			}
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, dispatcherElement, "failover");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, dispatcherElement, "max-subscribers");
		return builder;
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
		if (StringUtils.hasText(queueRef)) {
			builder.addConstructorArgReference(queueRef);
			return true;
		}
		return false;
	}

	private boolean parseStoreRef(BeanDefinitionBuilder builder, Element queueElement, String channel,
			boolean priority) {
		String storeRef = queueElement.getAttribute("message-store");
		if (StringUtils.hasText(storeRef)) {
			BeanDefinitionBuilder queueBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(MessageGroupQueue.class);
			queueBuilder.addConstructorArgReference(storeRef);
			queueBuilder.addConstructorArgValue(new TypedStringValue(storeRef).getValue() + ":" + channel);
			queueBuilder.addPropertyValue("priority", priority);
			parseQueueCapacity(queueBuilder, queueElement);
			builder.addConstructorArgValue(queueBuilder.getBeanDefinition());
			return true;
		}
		return false;
	}

}
