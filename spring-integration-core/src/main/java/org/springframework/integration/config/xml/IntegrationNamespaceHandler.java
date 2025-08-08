/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
	public void init() { // NOSONAR
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
		registerBeanDefinitionParser("stream-transformer", new StreamTransformerParser());
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
		registerBeanDefinitionParser("scatter-gather", new ScatterGatherParser());
		registerBeanDefinitionParser("idempotent-receiver", new IdempotentReceiverInterceptorParser());
		registerBeanDefinitionParser("management", new IntegrationManagementParser());
		registerBeanDefinitionParser("barrier", new BarrierParser());
	}

}
