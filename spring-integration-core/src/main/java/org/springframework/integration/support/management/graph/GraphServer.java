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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.endpoint.IntegrationConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.MessageChannel;

/**
 * Builds the runtime object model graph.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class GraphServer implements ApplicationContextAware, SmartInitializingSingleton {

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
	public void afterSingletonsInstantiated() {
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
		Map<String, MessageChannelNode> channelInfos = new HashMap<String, MessageChannelNode>();
		for (Entry<String, MessageChannel> entry : channels.entrySet()) {
			MessageChannel channel = entry.getValue();
			MessageChannelNode channelInfo = new MessageChannelNode(entry.getKey(), channel);
			String beanName = entry.getKey();
			nodes.add(channelInfo);
			channelInfos.put(beanName, channelInfo);
		}
		for (Entry<String, SourcePollingChannelAdapter> entry : spcas.entrySet()) {
			SourcePollingChannelAdapter adapter = entry.getValue();
			MessageSourceNode sourceInfo = sourceInfo(entry.getKey(), adapter);
			nodes.add(sourceInfo);
			MessageChannelNode channelInfo = channelInfos.get(sourceInfo.getOutput());
			if (channelInfo != null) {
				links.add(new LinkNode(sourceInfo.getNodeId(), channelInfo.getNodeId()));
			}
		}
		for (Entry<String, MessagingGatewaySupport> entry : gateways.entrySet()) {
			MessagingGatewaySupport gateway = entry.getValue();
			MessageGatewayNode gatewayInfo = gatewayInfo(entry.getKey(), gateway);
			nodes.add(gatewayInfo);
			MessageChannelNode channelInfo = channelInfos.get(gatewayInfo.getOutput());
			if (channelInfo != null) {
				links.add(new LinkNode(gatewayInfo.getNodeId(), channelInfo.getNodeId()));
			}
		}
		for (Entry<String, MessageProducerSupport> entry : producers.entrySet()) {
			MessageProducerSupport producer = entry.getValue();
			MessageProducerNode producerInfo = producerInfo(entry.getKey(), producer);
			nodes.add(producerInfo);
			MessageChannelNode channelInfo = channelInfos.get(producerInfo.getOutput());
			if (channelInfo != null) {
				links.add(new LinkNode(producerInfo.getNodeId(), channelInfo.getNodeId()));
			}
		}
		for (Entry<String, IntegrationConsumer> entry : consumers.entrySet()) {
			IntegrationConsumer consumer = entry.getValue();
			MessageHandlerNode handlerInfo = handlerInfo(entry.getKey(), consumer);
			nodes.add(handlerInfo);
			MessageChannelNode channelInfo = channelInfos.get(handlerInfo.getInput());
			if (channelInfo != null) {
				links.add(new LinkNode(channelInfo.getNodeId(), handlerInfo.getNodeId()));
			}
			if (handlerInfo.getOutput() != null) {
				channelInfo = channelInfos.get(handlerInfo.getOutput());
				if (channelInfo != null) {
					links.add(new LinkNode(handlerInfo.getNodeId(), channelInfo.getNodeId()));
				}
			}
		}
		this.graph = new Graph(nodes, links);
	}

	private MessageGatewayNode gatewayInfo(String name, MessagingGatewaySupport gateway) {
		return new MessageGatewayNode(name, gateway, gateway.getRequestChannelName());
	}

	private MessageProducerNode producerInfo(String name, MessageProducerSupport producer) {
		MessageChannel outputChannel = producer.getOutputChannel();
		String outputChannelName = outputChannel instanceof NamedComponent
				? ((NamedComponent) outputChannel).getComponentName() : "__unknown__";
		return new MessageProducerNode(name, producer, outputChannelName);
	}

	private MessageSourceNode sourceInfo(String name, SourcePollingChannelAdapter adapter) {
		return new MessageSourceNode(name, adapter.getMessageSource(), adapter.getOutputChannelName());
	}

	private MessageHandlerNode handlerInfo(String name, IntegrationConsumer consumer) {
		MessageChannel inputChannel = consumer.getInputChannel();
		MessageChannel outputChannel = consumer.getOutputChannel();
		String inputChannelName = inputChannel instanceof NamedComponent
				? ((NamedComponent) inputChannel).getComponentName() : "__unknown__";
		String outputChannelName = outputChannel == null ? null : outputChannel instanceof NamedComponent
				? ((NamedComponent) outputChannel).getComponentName() : "__unknown__";
		return new MessageHandlerNode(name, consumer.getHandler(), inputChannelName, outputChannelName);
	}

}
