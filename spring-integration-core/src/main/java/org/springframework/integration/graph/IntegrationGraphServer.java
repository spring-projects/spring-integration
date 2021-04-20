/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.graph;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.IntegrationConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.handler.CompositeMessageHandler;
import org.springframework.integration.handler.DiscardingMessageHandler;
import org.springframework.integration.router.RecipientListRouter.Recipient;
import org.springframework.integration.router.RecipientListRouterManagement;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.MappingMessageRouterManagement;
import org.springframework.integration.support.management.micrometer.MicrometerMetricsCaptorRegistrar;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;

/**
 * Builds the runtime object model graph.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class IntegrationGraphServer implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

	private static final float GRAPH_VERSION = 1.2f;

	private static MicrometerNodeEnhancer micrometerEnhancer;

	private final NodeFactory nodeFactory = new NodeFactory();

	private ApplicationContext applicationContext;

	private Graph graph;

	private String applicationName;

	private Function<NamedComponent, Map<String, Object>> additionalPropertiesCallback;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext; // NOSONAR (sync)
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;  // NOSONAR (sync)
	}

	/**
	 * Set the application name that will appear in the 'contentDescriptor' under
	 * the 'name' key. If not provided, the property 'spring.application.name' from
	 * the application context environment will be used (if present).
	 * @param applicationName the application name.
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName; //NOSONAR (sync)
	}

	/**
	 * Specify a callback {@link Function} to be called against each {@link NamedComponent}
	 * to populate additional properties to the target {@link IntegrationNode}.
	 * @param additionalPropertiesCallback the {@link Function} to use for properties.
	 * @since 5.1
	 */
	public void setAdditionalPropertiesCallback(@Nullable Function<NamedComponent,
			Map<String, Object>> additionalPropertiesCallback) {

		this.additionalPropertiesCallback = additionalPropertiesCallback;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().equals(this.applicationContext)) {
			buildGraph();
		}
	}

	/**
	 * Return the cached graph. Although the graph is cached, the data therein (stats
	 * etc.) are dynamic.
	 * @return the graph.
	 * @see #rebuild()
	 */
	public Graph getGraph() {
		if (this.graph == null) { //NOSONAR (sync)
			synchronized (this) {
				if (this.graph == null) {
					buildGraph();
				}
			}
		}
		return this.graph;
	}

	/**
	 * Rebuild the graph, re-cache it, and return it. Use this method if the application
	 * components have changed (added or removed).
	 * @return the graph.
	 * @see #getGraph()
	 */
	public Graph rebuild() {
		return buildGraph();
	}

	/**
	 * Get beans for the provided type from the application context.
	 * This method can be extended for some custom logic, e.g. get beans
	 * from the parent application context as well.
	 * @param type the type for beans to obtain
	 * @param <T> the type for beans to obtain
	 * @return a {@link Map} of bean for the provided type
	 * @since 5.1
	 */
	protected <T> Map<String, T> getBeansOfType(Class<T> type) {
		return this.applicationContext.getBeansOfType(type, true, false);
	}

	private synchronized Graph buildGraph() {
		if (micrometerEnhancer == null && MicrometerMetricsCaptorRegistrar.METER_REGISTRY_PRESENT) {
			micrometerEnhancer = new MicrometerNodeEnhancer(this.applicationContext);
		}
		String implementationVersion = IntegrationGraphServer.class.getPackage().getImplementationVersion();
		if (implementationVersion == null) {
			implementationVersion = "unknown - is Spring Integration running from the distribution jar?";
		}
		Map<String, Object> descriptor = new HashMap<>();
		descriptor.put("provider", "spring-integration");
		descriptor.put("providerVersion", implementationVersion);
		descriptor.put("providerFormatVersion", GRAPH_VERSION);
		String name = this.applicationName;
		if (name == null) {
			name = this.applicationContext.getEnvironment().getProperty("spring.application.name");
		}
		if (name != null) {
			descriptor.put("name", name);
		}
		this.nodeFactory.reset();
		Collection<IntegrationNode> nodes = new ArrayList<>();
		Collection<LinkNode> links = new ArrayList<>();
		Map<String, MessageChannelNode> channelNodes = channels(nodes);
		pollingAdapters(nodes, links, channelNodes);
		gateways(nodes, links, channelNodes);
		producers(nodes, links, channelNodes);
		consumers(nodes, links, channelNodes);
		this.graph = new Graph(descriptor, nodes, links);
		return this.graph;
	}

	private Map<String, MessageChannelNode> channels(Collection<IntegrationNode> nodes) {
		return getBeansOfType(MessageChannel.class)
				.entrySet()
				.stream()
				.map(e -> {
					MessageChannel messageChannel = e.getValue();
					MessageChannelNode messageChannelNode = this.nodeFactory.channelNode(e.getKey(), messageChannel);
					if (messageChannel instanceof NamedComponent) {
						messageChannelNode.addProperties(getAdditionalPropertiesIfAny((NamedComponent) messageChannel));
					}
					return messageChannelNode;
				})
				.peek(nodes::add)
				.collect(Collectors.toMap(MessageChannelNode::getName, Function.identity()));
	}

	private void pollingAdapters(Collection<IntegrationNode> nodes, Collection<LinkNode> links,
			Map<String, MessageChannelNode> channelNodes) {

		getBeansOfType(SourcePollingChannelAdapter.class)
				.entrySet()
				.stream()
				.map(e -> {
					SourcePollingChannelAdapter sourceAdapter = e.getValue();
					MessageSourceNode sourceNode = this.nodeFactory.sourceNode(e.getKey(), sourceAdapter);
					sourceNode.addProperties(getAdditionalPropertiesIfAny(sourceAdapter));
					return sourceNode;
				})
				.peek(nodes::add)
				.forEach(sourceNode -> producerLink(links, channelNodes, sourceNode));
	}

	private void gateways(Collection<IntegrationNode> nodes, Collection<LinkNode> links,
			Map<String, MessageChannelNode> channelNodes) {

		getBeansOfType(MessagingGatewaySupport.class)
				.entrySet()
				.stream()
				.map(e -> {
					MessagingGatewaySupport gateway = e.getValue();
					MessageGatewayNode gatewayNode = this.nodeFactory.gatewayNode(e.getKey(), gateway);
					gatewayNode.addProperties(getAdditionalPropertiesIfAny(gateway));
					return gatewayNode;
				})
				.peek(nodes::add)
				.forEach(gatewayNode -> producerLink(links, channelNodes, gatewayNode));

		Map<String, GatewayProxyFactoryBean> gpfbs = getBeansOfType(GatewayProxyFactoryBean.class);

		for (Entry<String, GatewayProxyFactoryBean> entry : gpfbs.entrySet()) {
			entry.getValue()
					.getGateways()
					.entrySet()
					.stream()
					.map(e -> {
						MessagingGatewaySupport gateway = e.getValue();
						Method method = e.getKey();

						String nodeName =
								entry.getKey().substring(1) + "." +
										method.getName() +
										"(" +
										Arrays.stream(method.getParameterTypes())
												.map(Class::getName)
												.collect(Collectors.joining(","))
										+ ")";

						MessageGatewayNode gatewayNode = this.nodeFactory.gatewayNode(nodeName, gateway);
						gatewayNode.addProperties(getAdditionalPropertiesIfAny(gateway));
						return gatewayNode;
					})
					.peek(nodes::add)
					.forEach(gatewayNode -> producerLink(links, channelNodes, gatewayNode));
		}
	}

	private void producers(Collection<IntegrationNode> nodes, Collection<LinkNode> links,
			Map<String, MessageChannelNode> channelNodes) {

		getBeansOfType(MessageProducerSupport.class)
				.entrySet()
				.stream()
				.map(e -> {
					MessageProducerSupport producer = e.getValue();
					MessageProducerNode producerNode = this.nodeFactory.producerNode(e.getKey(), producer);
					producerNode.addProperties(getAdditionalPropertiesIfAny(producer));
					return producerNode;
				})
				.peek(nodes::add)
				.forEach(producerNode -> producerLink(links, channelNodes, producerNode));
	}

	private void consumers(Collection<IntegrationNode> nodes, Collection<LinkNode> links,
			Map<String, MessageChannelNode> channelNodes) {

		getBeansOfType(IntegrationConsumer.class)
				.entrySet()
				.stream()
				.map(e -> {
					IntegrationConsumer consumer = e.getValue();
					MessageHandlerNode handlerNode =
							consumer instanceof PollingConsumer
									? this.nodeFactory.polledHandlerNode(e.getKey(), (PollingConsumer) consumer)
									: this.nodeFactory.handlerNode(e.getKey(), consumer);
					handlerNode.addProperties(getAdditionalPropertiesIfAny(consumer));
					return handlerNode;
				})
				.peek(nodes::add)
				.forEach(handlerNode -> {
					MessageChannelNode channelNode = channelNodes.get(handlerNode.getInput());
					if (channelNode != null) {
						links.add(new LinkNode(channelNode.getNodeId(), handlerNode.getNodeId(), LinkNode.Type.input));
					}
					producerLink(links, channelNodes, handlerNode);
				});
	}

	@Nullable
	private Map<String, Object> getAdditionalPropertiesIfAny(NamedComponent namedComponent) {
		if (this.additionalPropertiesCallback != null) {
			return this.additionalPropertiesCallback.apply(namedComponent);
		}
		else {
			return null;
		}
	}

	private void producerLink(Collection<LinkNode> links, Map<String, MessageChannelNode> channelNodes,
			EndpointNode endpointNode) {

		MessageChannelNode channelNode;
		if (endpointNode.getOutput() != null) {
			channelNode = channelNodes.get(endpointNode.getOutput());
			if (channelNode != null) {
				links.add(new LinkNode(endpointNode.getNodeId(), channelNode.getNodeId(), LinkNode.Type.output));
			}
		}
		if (endpointNode instanceof ErrorCapableNode) {
			channelNode = channelNodes.get(((ErrorCapableNode) endpointNode).getErrors());
			if (channelNode != null) {
				links.add(new LinkNode(endpointNode.getNodeId(), channelNode.getNodeId(), LinkNode.Type.error));
			}
		}
		if (endpointNode instanceof DiscardingMessageHandlerNode) {
			channelNode = channelNodes.get(((DiscardingMessageHandlerNode) endpointNode).getDiscards());
			if (channelNode != null) {
				links.add(new LinkNode(endpointNode.getNodeId(), channelNode.getNodeId(), LinkNode.Type.discard));
			}
		}
		if (endpointNode instanceof RoutingMessageHandlerNode) {
			Collection<String> routes = ((RoutingMessageHandlerNode) endpointNode).getRoutes();
			for (String route : routes) {
				channelNode = channelNodes.get(route);
				if (channelNode != null) {
					links.add(new LinkNode(endpointNode.getNodeId(), channelNode.getNodeId(), LinkNode.Type.route));
				}
			}
		}
	}

	private static final class NodeFactory {

		private final AtomicInteger nodeId = new AtomicInteger();

		NodeFactory() {
		}

		MessageChannelNode channelNode(String name, MessageChannel channel) {
			MessageChannelNode node;
			if (channel instanceof PollableChannel) {
				node = new PollableChannelNode(this.nodeId.incrementAndGet(), name, channel);
			}
			else {
				node = new MessageChannelNode(this.nodeId.incrementAndGet(), name, channel);
			}
			if (IntegrationGraphServer.micrometerEnhancer != null) {
				node = IntegrationGraphServer.micrometerEnhancer.enhance(node);
			}
			return node;
		}

		MessageGatewayNode gatewayNode(String name, MessagingGatewaySupport gateway) {
			String errorChannel = channelToBeanName(gateway.getErrorChannel());
			String requestChannel = channelToBeanName(gateway.getRequestChannel());
			return new MessageGatewayNode(this.nodeId.incrementAndGet(), name, gateway, requestChannel, errorChannel);
		}

		@Nullable
		private String channelToBeanName(MessageChannel messageChannel) {
			return messageChannel instanceof NamedComponent
					? ((NamedComponent) messageChannel).getBeanName()
					: Objects.toString(messageChannel, null);
		}

		MessageProducerNode producerNode(String name, MessageProducerSupport producer) {
			String errorChannel = channelToBeanName(producer.getErrorChannel());
			String outputChannel = channelToBeanName(producer.getOutputChannel());
			return new MessageProducerNode(this.nodeId.incrementAndGet(), name, producer,
					outputChannel, errorChannel);
		}

		MessageSourceNode sourceNode(String name, SourcePollingChannelAdapter adapter) {
			String errorChannel = channelToBeanName(adapter.getDefaultErrorChannel());
			String outputChannel = channelToBeanName(adapter.getOutputChannel());
			String nameToUse = name;
			MessageSource<?> source = adapter.getMessageSource();
			if (source instanceof NamedComponent) {
				nameToUse = IntegrationUtils.obtainComponentName((NamedComponent) source);
			}
			MessageSourceNode node = new MessageSourceNode(this.nodeId.incrementAndGet(), nameToUse, source,
					outputChannel, errorChannel);
			if (IntegrationGraphServer.micrometerEnhancer != null) {
				node = IntegrationGraphServer.micrometerEnhancer.enhance(node);
			}
			return node;
		}

		MessageHandlerNode handlerNode(String nameArg, IntegrationConsumer consumer) {
			String outputChannelName = channelToBeanName(consumer.getOutputChannel());
			MessageHandler handler = consumer.getHandler();
			MessageHandlerNode node;
			String name = nameArg;
			if (handler instanceof NamedComponent) {
				name = IntegrationUtils.obtainComponentName((NamedComponent) handler);
			}
			if (handler instanceof CompositeMessageHandler) {
				node = compositeHandler(name, consumer, (CompositeMessageHandler) handler, outputChannelName, null,
						false);
			}
			else if (handler instanceof DiscardingMessageHandler) {
				node = discardingHandler(name, consumer, (DiscardingMessageHandler) handler, outputChannelName, null,
						false);
			}
			else if (handler instanceof MappingMessageRouterManagement) {
				node = routingHandler(name, consumer, handler, (MappingMessageRouterManagement) handler,
						outputChannelName, null, false);
			}
			else if (handler instanceof RecipientListRouterManagement) {
				node = recipientListRoutingHandler(name, consumer, handler, (RecipientListRouterManagement) handler,
						outputChannelName, null, false);
			}
			else {
				String inputChannel = channelToBeanName(consumer.getInputChannel());
				node = new MessageHandlerNode(this.nodeId.incrementAndGet(), name, handler,
						inputChannel, outputChannelName);
			}
			if (IntegrationGraphServer.micrometerEnhancer != null) {
				node = IntegrationGraphServer.micrometerEnhancer.enhance(node);
			}
			return node;
		}

		MessageHandlerNode polledHandlerNode(String nameArg, PollingConsumer consumer) {
			String outputChannelName = channelToBeanName(consumer.getOutputChannel());
			String errorChannel = channelToBeanName(consumer.getDefaultErrorChannel());
			MessageHandler handler = consumer.getHandler();
			MessageHandlerNode node;
			String name = nameArg;
			if (handler instanceof NamedComponent) {
				name = IntegrationUtils.obtainComponentName((NamedComponent) handler);
			}
			if (handler instanceof CompositeMessageHandler) {
				node = compositeHandler(name, consumer, (CompositeMessageHandler) handler, outputChannelName,
						errorChannel, true);
			}
			else if (handler instanceof DiscardingMessageHandler) {
				node = discardingHandler(name, consumer, (DiscardingMessageHandler) handler, outputChannelName,
						errorChannel, true);
			}
			else if (handler instanceof MappingMessageRouterManagement) {
				node = routingHandler(name, consumer, handler, (MappingMessageRouterManagement) handler,
						outputChannelName, errorChannel, true);
			}
			else if (handler instanceof RecipientListRouterManagement) {
				node = recipientListRoutingHandler(name, consumer, handler, (RecipientListRouterManagement) handler,
						outputChannelName, errorChannel, true);
			}
			else {
				String inputChannel = channelToBeanName(consumer.getInputChannel());
				node = new ErrorCapableMessageHandlerNode(this.nodeId.incrementAndGet(), name, handler,
						inputChannel, outputChannelName, errorChannel);
			}
			if (IntegrationGraphServer.micrometerEnhancer != null) {
				node = IntegrationGraphServer.micrometerEnhancer.enhance(node);
			}
			return node;
		}

		MessageHandlerNode compositeHandler(String name, IntegrationConsumer consumer,
				CompositeMessageHandler handler, String output, String errors, boolean polled) {

			List<CompositeMessageHandlerNode.InnerHandler> innerHandlers =
					handler.getHandlers()
							.stream()
							.filter(NamedComponent.class::isInstance)
							.map(NamedComponent.class::cast)
							.map(named ->
									new CompositeMessageHandlerNode.InnerHandler(
											IntegrationUtils.obtainComponentName(named),
											named.getComponentType()))
							.collect(Collectors.toList());

			String inputChannel = channelToBeanName(consumer.getInputChannel());
			return polled
					? new ErrorCapableCompositeMessageHandlerNode(this.nodeId.incrementAndGet(), name, handler,
					inputChannel, output, errors, innerHandlers)
					: new CompositeMessageHandlerNode(this.nodeId.incrementAndGet(), name, handler,
					inputChannel, output, innerHandlers);
		}

		MessageHandlerNode discardingHandler(String name, IntegrationConsumer consumer,
				DiscardingMessageHandler handler, String output, String errors, boolean polled) {

			String discards = channelToBeanName(handler.getDiscardChannel());
			String inputChannel = channelToBeanName(consumer.getInputChannel());
			return polled
					? new ErrorCapableDiscardingMessageHandlerNode(this.nodeId.incrementAndGet(), name, handler,
					inputChannel, output, discards, errors)
					: new DiscardingMessageHandlerNode(this.nodeId.incrementAndGet(), name, handler,
					inputChannel, output, discards);
		}

		MessageHandlerNode routingHandler(String name, IntegrationConsumer consumer, MessageHandler handler,
				MappingMessageRouterManagement router, String output, String errors, boolean polled) {

			Collection<String> routes =
					Stream.concat(router.getChannelMappings().values().stream(),
							router.getDynamicChannelNames().stream())
							.collect(Collectors.toList());

			String inputChannel = channelToBeanName(consumer.getInputChannel());
			return polled
					? new ErrorCapableRoutingNode(this.nodeId.incrementAndGet(), name, handler,
					inputChannel, output, errors, routes)
					: new RoutingMessageHandlerNode(this.nodeId.incrementAndGet(), name, handler,
					inputChannel, output, routes);
		}

		MessageHandlerNode recipientListRoutingHandler(String name, IntegrationConsumer consumer,
				MessageHandler handler, RecipientListRouterManagement router, String output, String errors,
				boolean polled) {

			List<String> routes =
					router.getRecipients()
							.stream()
							.map(recipient -> channelToBeanName(((Recipient) recipient).getChannel()))
							.collect(Collectors.toList());

			String inputChannel = channelToBeanName(consumer.getInputChannel());
			return polled
					? new ErrorCapableRoutingNode(this.nodeId.incrementAndGet(), name, handler,
					inputChannel, output, errors, routes)
					: new RoutingMessageHandlerNode(this.nodeId.incrementAndGet(), name, handler,
					inputChannel, output, routes);
		}

		void reset() {
			this.nodeId.set(0);
		}

	}

}
