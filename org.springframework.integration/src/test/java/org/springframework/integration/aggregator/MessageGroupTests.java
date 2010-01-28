package org.springframework.integration.aggregator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageGroupTests {

    @Mock
    private MessageStore store;

    private Object key = new Object();

    @Mock
    private MessageGroupListener listener;

    @Mock
    private CompletionStrategy completionStrategy;

    private MessageGroup group;

    @Before
    public void buildMessageGroup() {
        group = new MessageGroup(Collections.<Message<?>>emptyList(), completionStrategy, key, listener);
    }

    @Test
    public void shouldFindSupersedingMessages() {
        final Message<?> message1 = MessageBuilder.withPayload("test").setCorrelationId("foo").build();
        final Message<?> message2 = MessageBuilder.fromMessage(message1).build();
        assertThat(group.hasNoMessageSuperseding(message1), is(true));
        group.add(message2);
        assertThat(group.hasNoMessageSuperseding(message1), is(false));
    }
}
