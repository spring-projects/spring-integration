/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
