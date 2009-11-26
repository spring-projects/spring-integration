package org.springframework.integration.aggregator;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.aggregator.BufferedMessagesCallback;

import java.util.Collection;

/**
 * @author Iwein Fuld
 */
public interface MessageGroupProcessor {

    void processAndSend(Object correlationKey,
                        Collection<Message<?>> messagesUpForProcessing,
                        MessageChannel outputChannel,
                        BufferedMessagesCallback processedCallback);
}