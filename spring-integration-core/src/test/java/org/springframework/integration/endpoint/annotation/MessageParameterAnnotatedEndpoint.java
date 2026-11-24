/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
