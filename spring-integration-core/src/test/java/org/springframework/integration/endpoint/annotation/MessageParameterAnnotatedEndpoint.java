/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.endpoint.annotation;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@MessageEndpoint
public class MessageParameterAnnotatedEndpoint {

	@ServiceActivator(inputChannel = "inputChannel", outputChannel = "outputChannel")
	public Message<String> sayHello(Message<?> message) {
		return new GenericMessage<>("hello " + message.getPayload());
	}

}
