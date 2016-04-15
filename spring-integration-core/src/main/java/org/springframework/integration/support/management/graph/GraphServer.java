/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.support.management.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.endpoint.IntegrationConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.MessageChannel;

/**
 * Builds the runtime object model graph.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class GraphServer implements ApplicationContextAware, SmartInitializingSingleton {

	private final NodeFactory nodeFactory = new NodeFactory();

	private ApplicationContext applicationContext;

	private Graph graph;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public Graph getGraph() {
		return this.graph;
	}

	@Override
	public synchronized void afterSingletonsInstantiated() {
		Map<String, MessageChannel> channels = this.applicationContext
				.getBeansOfType(MessageChannel.class);
		Map<String, SourcePollingChannelAdapter> spcas = this.applicationContext
				.getBeansOfType(SourcePollingChannelAdapter.class);
		Map<String, MessagingGatewaySupport> gateways = this.applicationContext
				.getBeansOfType(MessagingGatewaySupport.class);
		Map<String, MessageProducerSupport> producers = this.applicationContext
				.getBeansOfType(MessageProducerSupport.class);
		Map<String, IntegrationConsumer> consumers = this.applicationContext.getBeansOfType(IntegrationConsumer.class);
		Collection<IntegrationNode> nodes = new ArrayList<IntegrationNode>();
		Collection<LinkNode> links = new ArrayList<LinkNode>();
		Map<String, MessageChannelNode> channelNodes = new HashMap<String, MessageChannelNode>();
		for (Entry<String, MessageChannel> entry : channels.entrySet()) {
			MessageChannel channel = entry.getValue();
			MessageChannelNode channelNode = this.nodeFactory.channelNode(entry.getKey(), channel);
			String beanName = entry.getKey();
			nodes.add(channelNode);
			channelNodes.put(beanName, channelNode);
		}
		for (Entry<String, SourcePollingChannelAdapter> entry : spcas.entrySet()) {
			SourcePollingChannelAdapter adapter = entry.getValue();
			MessageSourceNode sourceNode = this.nodeFactory.sourceNode(entry.getKey(), adapter);
			nodes.add(sourceNode);
			MessageChannelNode channelNode = channelNodes.get(sourceNode.getOutput());
			if (channelNode != null) {
				links.add(new LinkNode(sourceNode.getNodeId(), channelNode.getNodeId()));
			}
		}
		for (Entry<String, MessagingGatewaySupport> entry : gateways.entrySet()) {
			MessagingGatewaySupport gateway = entry.getValue();
			MessageGatewayNode gatewayNode = this.nodeFactory.gatewayNode(entry.getKey(), gateway);
			nodes.add(gatewayNode);
			MessageChannelNode channelInfo = channelNodes.get(gatewayNode.getOutput());
			if (channelInfo != null) {
				links.add(new LinkNode(gatewayNode.getNodeId(), channelInfo.getNodeId()));
			}
		}
		for (Entry<String, MessageProducerSupport> entry : producers.entrySet()) {
			MessageProducerSupport producer = entry.getValue();
			MessageProducerNode producerNode = this.nodeFactory.producerNode(entry.getKey(), producer);
			nodes.add(producerNode);
			MessageChannelNode channelNode = channelNodes.get(producerNode.getOutput());
			if (channelNode != null) {
				links.add(new LinkNode(producerNode.getNodeId(), channelNode.getNodeId()));
			}
		}
		for (Entry<String, IntegrationConsumer> entry : consumers.entrySet()) {
			IntegrationConsumer consumer = entry.getValue();
			MessageHandlerNode handlerNode = this.nodeFactory.handlerNode(entry.getKey(), consumer);
			nodes.add(handlerNode);
			MessageChannelNode channelNode = channelNodes.get(handlerNode.getInput());
			if (channelNode != null) {
				links.add(new LinkNode(channelNode.getNodeId(), handlerNode.getNodeId()));
			}
			if (handlerNode.getOutput() != null) {
				channelNode = channelNodes.get(handlerNode.getOutput());
				if (channelNode != null) {
					links.add(new LinkNode(handlerNode.getNodeId(), channelNode.getNodeId()));
				}
			}
		}
		this.graph = new Graph(nodes, links);
	}

	/**
	 * Rebuild the graph if new components have been added.
	 */
	public void rebuild() {
		this.nodeFactory.nodeId.set(0);
		afterSingletonsInstantiated();
	}

	private final static class NodeFactory {

		private final AtomicInteger nodeId = new AtomicInteger();

		private MessageChannelNode channelNode(String name, MessageChannel channel) {
			return new MessageChannelNode(this.nodeId.incrementAndGet(), name, channel);
		}

		private MessageGatewayNode gatewayNode(String name, MessagingGatewaySupport gateway) {
			return new MessageGatewayNode(this.nodeId.incrementAndGet(), name, gateway,
					gateway.getRequestChannel().toString());
		}

		private MessageProducerNode producerNode(String name, MessageProducerSupport producer) {
			MessageChannel outputChannel = producer.getOutputChannel();
			String outputChannelName = outputChannel == null ? "__unknown__" : outputChannel.toString();
			return new MessageProducerNode(this.nodeId.incrementAndGet(), name, producer, outputChannelName);
		}

		private MessageSourceNode sourceNode(String name, SourcePollingChannelAdapter adapter) {
			return new MessageSourceNode(this.nodeId.incrementAndGet(), name, adapter.getMessageSource(),
					adapter.getOutputChannel().toString());
		}

		private MessageHandlerNode handlerNode(String name, IntegrationConsumer consumer) {
			MessageChannel outputChannel = consumer.getOutputChannel();
			String outputChannelName = outputChannel == null ? null : outputChannel.toString();
			return new MessageHandlerNode(this.nodeId.incrementAndGet(), name, consumer.getHandler(),
					consumer.getInputChannel().toString(), outputChannelName);
		}

	}

}
