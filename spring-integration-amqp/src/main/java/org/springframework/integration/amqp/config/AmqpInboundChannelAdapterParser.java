/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.amqp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.util.StringUtils;

/**
 * Parser for the AMQP 'inbound-channel-adapter' element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class AmqpInboundChannelAdapterParser extends AbstractAmqpInboundAdapterParser {

	AmqpInboundChannelAdapterParser() {
		super(AmqpInboundChannelAdapter.class.getName());
	}

	@Override
	protected final String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		String id = element.getAttribute("id");
		if (!element.hasAttribute("channel")) {
			// the created channel will get the 'id', so the adapter's bean name includes a suffix
			id = id + ".adapter";
		}
		else if (!StringUtils.hasText(id)) {
			id = parserContext.getReaderContext().generateBeanName(definition);
		}
		return id;
	}

	@Override
	protected void configureChannels(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String channelName = element.getAttribute("channel");
		if (!StringUtils.hasText(channelName)) {
			channelName = this.createDirectChannel(element, parserContext);
		}
		builder.addPropertyReference("outputChannel", channelName);
	}

	private String createDirectChannel(Element element, ParserContext parserContext) {
		String channelId = element.getAttribute("id");
		if (!StringUtils.hasText(channelId)) {
			parserContext.getReaderContext().error("The channel-adapter's 'id' attribute is required when no 'channel' "
					+ "reference has been provided, because that 'id' would be used for the created channel.", element);
		}
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class);
		BeanDefinitionHolder holder = new BeanDefinitionHolder(channelBuilder.getBeanDefinition(), channelId);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		return channelId;
	}

}
