/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.store;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for non-destructive access through the basic store contract.
 *
 * @author Fan Kongyuan
 *
 * @since 7.3
 */
class BasicMessageGroupStoreTests {

	@Test
	void existingStoresCanUseDefaultReadOperations() {
		BasicMessageGroupStore store = mock(BasicMessageGroupStore.class, CALLS_REAL_METHODS);
		Message<?> message = new GenericMessage<>("queued message");
		when(store.getMessageGroup("channel")).thenReturn(new SimpleMessageGroup(List.of(message), "channel"));

		assertThat(store.getMessagesForGroup("channel")).containsExactly(message);
		try (Stream<Message<?>> messages = store.streamMessagesForGroup("channel")) {
			assertThat(messages).containsExactly(message);
		}
	}

	@Test
	void persistentFactoryAcceptsBasicStoreAndClosesSingleMessageStream() {
		BasicMessageGroupStore store = mock(BasicMessageGroupStore.class);
		SimpleMessageGroupFactory factory = new SimpleMessageGroupFactory(SimpleMessageGroupFactory.GroupType.PERSISTENT);
		MessageGroup group = factory.create(store, "channel");
		verifyNoInteractions(store);

		Message<?> message = new GenericMessage<>("queued message");
		AtomicBoolean closed = new AtomicBoolean();
		when(store.streamMessagesForGroup("channel"))
				.thenReturn(Stream.<Message<?>>of(message).onClose(() -> closed.set(true)));

		assertThat(group.getOne()).isEqualTo(message);
		assertThat(closed).isTrue();
	}

}
