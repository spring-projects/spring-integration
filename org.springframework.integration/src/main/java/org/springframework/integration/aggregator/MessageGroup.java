package org.springframework.integration.aggregator;

import org.springframework.integration.core.Message;

import java.util.*;

/**
 * Represents an mutable group of correlated messages that is bound to a certain {@link org.springframework.integration.store.MessageStore}
 * and correlation key. The group will grow during its lifetime, when messages are <code>add</code>ed to it. <strong>
 * This is not thread safe and should not be used for long running aggregations</strong>.
 * <p/>
 * According to its {@link org.springframework.integration.aggregator.CompletionStrategy} it can be <i>complete</i>
 * depending on the messages in the group.
 * <p/>
 * Optionally MessageGroupListeners can be added to get callbacks when (parts of) the group are processed or the whole
 * group is completed.
 *
 *
 *
 * @author Iwein Fuld
 */
public class MessageGroup {
    private final CompletionStrategy completionStrategy;
    private final Object correlationKey;
    private final ArrayList<Message<?>> messages = new ArrayList<Message<?>>();
    private final List<MessageGroupListener> listeners;


    public MessageGroup(Collection<? extends Message<?>> originalMessages, CompletionStrategy completionStrategy, Object correlationKey,
                          MessageGroupListener... listeners) {
        this.completionStrategy = completionStrategy;
        this.correlationKey = correlationKey;
        this.messages.addAll(originalMessages);
        this.listeners = Collections.unmodifiableList(Arrays.asList(listeners));
    }

    public boolean hasNoMessageSuperseding(Message<?> message) {
        Integer messageSequenceNumber = message.getHeaders().getSequenceNumber();
        if (messageSequenceNumber == null) {
            return true;
        }
        for (Message<?> member : messages) {
            Integer memberSequenceNumber = member.getHeaders().getSequenceNumber();
            if (memberSequenceNumber == messageSequenceNumber) {
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
}
