package org.springframework.integration.aggregator;

import org.springframework.integration.core.Message;

import java.util.*;

/**
 * Represents an mutable group of correlated messages that is bound to a certain
 * {@link org.springframework.integration.store.MessageStore}
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
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 */
public class MessageGroup {
    private final CompletionStrategy completionStrategy;
    private final Object correlationKey;
    private final ArrayList<Message<?>> messages = new ArrayList<Message<?>>();
    private final List<MessageGroupListener> listeners;


    public MessageGroup(Collection<? extends Message<?>> originalMessages, CompletionStrategy completionStrategy,
                        Object correlationKey, MessageGroupListener... listeners) {
        this.completionStrategy = completionStrategy;
        this.correlationKey = correlationKey;
        this.messages.addAll(originalMessages);
        this.listeners = Collections.unmodifiableList(Arrays.asList(listeners));
    }

    /**
     * This method determines whether messages have been added to this group that supersede the given message based on
     * its sequence id. This can be helpful to avoid ending up with sequences larger than their required sequence size,
     * or sequences that are missing certain sequence numbers.
     */
    public boolean hasNoMessageSuperseding(Message<?> message) { 	
        Integer messageSequenceNumber = message.getHeaders().getSequenceNumber();
        if (messageSequenceNumber > 0){
        	if (messageSequenceNumber == null) {
                return true;
            }
            for (Message<?> member : messages) {
                Integer memberSequenceNumber = member.getHeaders().getSequenceNumber();
                if (memberSequenceNumber.equals(messageSequenceNumber)) {
                    return false;
                }
            }
    	} 
        return true;
    }

    /**
     * Add a message to the internal list. This is needed to avoid hitting the underlying store or copying the internal
     * list. Use with care.
     */
    protected void add(Message<?> message) {
        messages.add(message);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isComplete() {
        return completionStrategy.isComplete(messages);
    }

    /**
     * @return internal message list, modification is allowed, but not recommended
     */
    public List<Message<?>> getMessages() {
        return messages;
    }

    /**
     * @return the correlation key that links these messages together according to a particular CorrelationStrategy
     */
    public Object getCorrelationKey() {
        return correlationKey;
    }

    /**
     * Call this method to sign off on processing of certain messages e.g. from a MessageProcessor. Typically this will
     * remove these messages from the processing backlog.
     */
    public void onProcessingOf(Message... messages) {
        for (MessageGroupListener listener : listeners) {
            listener.onProcessingOf(messages);
        }
    }

    /**
     * Call this method to signal the completion of the processing of an entire group
     */
    public void onCompletion() {
        for (MessageGroupListener listener : listeners) {
            listener.onCompletionOf(correlationKey);
        }
    }
}
