/*
 * Copyright 2002-2014 the original author or authors.
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

/**
 * Namespace handler for the integration namespace.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 */
public class IntegrationNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("channel", new PointToPointChannelParser());
		registerBeanDefinitionParser("publish-subscribe-channel", new PublishSubscribeChannelParser());
		registerBeanDefinitionParser("service-activator", new ServiceActivatorParser());
		registerBeanDefinitionParser("transformer", new TransformerParser());
		registerBeanDefinitionParser("enricher", new EnricherParser());
		registerBeanDefinitionParser("filter", new FilterParser());
		registerBeanDefinitionParser("router", new DefaultRouterParser());
		registerBeanDefinitionParser("header-value-router", new HeaderValueRouterParser());
		registerBeanDefinitionParser("payload-type-router", new PayloadTypeRouterParser());
		registerBeanDefinitionParser("exception-type-router", new ErrorMessageExceptionTypeRouterParser());
		registerBeanDefinitionParser("recipient-list-router", new RecipientListRouterParser());
		registerBeanDefinitionParser("splitter", new SplitterParser());
		registerBeanDefinitionParser("aggregator", new AggregatorParser());
		registerBeanDefinitionParser("resequencer", new ResequencerParser());
		registerBeanDefinitionParser("header-enricher", new StandardHeaderEnricherParser());
		registerBeanDefinitionParser("header-filter", new HeaderFilterParser());
		registerBeanDefinitionParser("object-to-string-transformer", new ObjectToStringTransformerParser());
		registerBeanDefinitionParser("object-to-map-transformer", new ObjectToMapTransformerParser());
		registerBeanDefinitionParser("map-to-object-transformer", new MapToObjectTransformerParser());
		registerBeanDefinitionParser("object-to-json-transformer", new ObjectToJsonTransformerParser());
		registerBeanDefinitionParser("json-to-object-transformer", new JsonToObjectTransformerParser());
		registerBeanDefinitionParser("payload-serializing-transformer", new PayloadSerializingTransformerParser());
		registerBeanDefinitionParser("payload-deserializing-transformer", new PayloadDeserializingTransformerParser());
		registerBeanDefinitionParser("claim-check-in", new ClaimCheckInParser());
		registerBeanDefinitionParser("syslog-to-map-transformer", new SyslogToMapTransformerParser());
		registerBeanDefinitionParser("claim-check-out", new ClaimCheckOutParser());
		registerBeanDefinitionParser("inbound-channel-adapter", new DefaultInboundChannelAdapterParser());
		registerBeanDefinitionParser("resource-inbound-channel-adapter", new ResourceInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new DefaultOutboundChannelAdapterParser());
		registerBeanDefinitionParser("logging-channel-adapter", new LoggingChannelAdapterParser());
		registerBeanDefinitionParser("gateway", new GatewayParser());
		registerBeanDefinitionParser("delayer", new DelayerParser());
		registerBeanDefinitionParser("bridge", new BridgeParser());
		registerBeanDefinitionParser("chain", new ChainParser());
		registerBeanDefinitionParser("selector", new SelectorParser());
		registerBeanDefinitionParser("selector-chain", new SelectorChainParser());
		registerBeanDefinitionParser("poller", new PollerParser());
		registerBeanDefinitionParser("annotation-config", new AnnotationConfigParser());
		registerBeanDefinitionParser("application-event-multicaster", new ApplicationEventMulticasterParser());
		registerBeanDefinitionParser("publishing-interceptor", new PublishingInterceptorParser());
		registerBeanDefinitionParser("channel-interceptor", new GlobalChannelInterceptorParser());
		registerBeanDefinitionParser("converter", new ConverterParser());
		registerBeanDefinitionParser("message-history", new MessageHistoryParser());
		registerBeanDefinitionParser("control-bus", new ControlBusParser());
		registerBeanDefinitionParser("wire-tap", new GlobalWireTapParser());
		registerBeanDefinitionParser("transaction-synchronization-factory", new TransactionSynchronizationFactoryParser());
		registerBeanDefinitionParser("spel-function", new SpelFunctionParser());
		registerBeanDefinitionParser("spel-property-accessors", new SpelPropertyAccessorsParser());
		RetryAdviceParser retryParser = new RetryAdviceParser();
		registerBeanDefinitionParser("handler-retry-advice", retryParser);
		registerBeanDefinitionParser("retry-advice", retryParser);
	}

}
