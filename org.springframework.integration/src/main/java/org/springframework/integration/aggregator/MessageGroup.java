package org.springframework.integration.aggregator;

import org.springframework.integration.core.Message;
import org.springframework.integration.store.MessageStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of correlated messages that is bound to a certain
 * {@link org.springframework.integration.store.MessageStore} and correlation key.
 * The group can grow during its lifetime, if messages are <code>add</code>ed to it.
 * <p/>
 * According to its {@link org.springframework.integration.aggregator.CompletionStrategy} it
 * can be <i>complete</i> depending on the messages in the group.
 * <p/>
 * Listeners can be configured to get callbacks when (parts of) the group are processed or the whole group is completed.
 *
 * @author Iwein Fuld
 */
public class MessageGroup {
    private final MessageStore store;
    private final CompletionStrategy completionStrategy;
    private final Object correlationKey;
    private final ArrayList<Message<?>> messages = new ArrayList<Message<?>>();
    private final List<MessageGroupListener> listeners;


    public MessageGroup(MessageStore store, CompletionStrategy completionStrategy, Object correlationKey, MessageGroupListener... listeners) {
        this.store = store;
        this.completionStrategy = completionStrategy;
        this.correlationKey = correlationKey;
        this.messages.addAll(store.list(correlationKey));
        this.listeners = Collections.unmodifiableList(Arrays.asList(listeners));
    }

    public boolean hasNoMessageSuperseding(Message<?> message) {
        for (Message<?> member : messages) {
            if (member.getHeaders().getSequenceNumber() == message.getHeaders().getSequenceNumber()) {
                return false;
            }
        }
        return true;
    }

    public void add(Message<?> message) {
        messages.add(message);
    }

    public boolean isComplete() {
        return completionStrategy.isComplete(messages);
    }

    public static MessageGroupBuilder builder() {
        return new MessageGroupBuilder();
    }

    public List<Message<?>> getMessages() {
        return messages;
    }

    public Object getCorrelationKey() {
        return correlationKey;
    }

    public void onProcessingOf(Message... messages) {
        for (MessageGroupListener listener : listeners) {
            listener.onProcessingOf(messages);
        }
    }

    public void onCompletion() {
        for (MessageGroupListener listener : listeners) {
            listener.onCompletionOf(correlationKey);
        }
    }

    final static class MessageGroupBuilder {
        private MessageStore store;
        private Object correlationKey;
        private MessageGroupListener[] listeners;
        private CompletionStrategy completionStrategy;

        public MessageGroupBuilder withCorrelationKey(Object key) {
            this.correlationKey = key;
            return this;
        }

        public MessageGroupBuilder withStore(MessageStore store) {
            this.store = store;
            return this;
        }

        public MessageGroupBuilder observedBy(MessageGroupListener... listeners) {
            this.listeners = listeners;
            return this;
        }

        public MessageGroupBuilder completedBy(CompletionStrategy completionStrategy) {
            this.completionStrategy = completionStrategy;
            return this;
        }

        public MessageGroup build() {
            return new MessageGroup(store, completionStrategy, correlationKey, listeners);
        }
    }
}
