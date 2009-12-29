package org.springframework.integration.aggregator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.integration.store.MessageStore;

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

    @Test
    public void shouldBuildMessageGroup() {
        MessageGroup group = MessageGroup.builder().
                withStore(store).withCorrelationKey(key).
                completedBy(completionStrategy).observedBy(listener).
                build();
    }
}
