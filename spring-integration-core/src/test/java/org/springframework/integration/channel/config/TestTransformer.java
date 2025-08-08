/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel.config;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class TestTransformer implements Transformer {

	public Message<?> transform(Message<?> message) {
		return MessageBuilder.withPayload(message.getPayload().toString().toUpperCase()).build();
	}

}
