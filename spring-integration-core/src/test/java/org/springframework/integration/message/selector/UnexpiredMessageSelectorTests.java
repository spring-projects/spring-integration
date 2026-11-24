/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.message.selector;

import org.junit.jupiter.api.Test;

import org.springframework.integration.selector.UnexpiredMessageSelector;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class UnexpiredMessageSelectorTests {

	@Test
	public void testExpiredMessageRejected() {
		long past = System.currentTimeMillis() - 60000;
		Message<String> message = MessageBuilder.withPayload("expired")
				.setExpirationDate(past).build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertThat(selector.accept(message)).isFalse();
	}

	@Test
	public void testUnexpiredMessageAccepted() {
		long future = System.currentTimeMillis() + 60000;
		Message<String> message = MessageBuilder.withPayload("unexpired")
				.setExpirationDate(future).build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertThat(selector.accept(message)).isTrue();
	}

	@Test
	public void testMessageWithNullExpirationDateNeverExpires() {
		Message<String> message = MessageBuilder.withPayload("unexpired").build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertThat(selector.accept(message)).isTrue();
	}

}
