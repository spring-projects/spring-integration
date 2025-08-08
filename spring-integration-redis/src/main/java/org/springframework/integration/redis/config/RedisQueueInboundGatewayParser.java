/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.inbound.RedisQueueInboundGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;queue-inbound-gateway&gt; element of the 'redis' namespace.
 *
 * @author David Liu
 * @author Artem Bilan
 * @since 4.1
 */
public class RedisQueueInboundGatewayParser extends AbstractInboundGatewayParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return RedisQueueInboundGateway.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !attributeName.equals("queue") // NOSONAR boolean complexity
				&& !attributeName.equals("connection-factory")
				&& !attributeName.equals("serializer")
				&& !attributeName.equals("task-executor")
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		builder.addConstructorArgValue(element.getAttribute("queue"));
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer", true);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "recovery-interval");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
	}

}
