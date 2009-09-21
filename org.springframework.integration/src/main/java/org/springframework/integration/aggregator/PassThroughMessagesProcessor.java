package org.springframework.integration.aggregator;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;

import java.util.Collection;

public class PassThroughMessagesProcessor implements MessagesProcessor {

    public void processAndSend(Object correlationKey, Collection<Message<?>> messagesUpForProcessing, MessageChannel outputChannel, BufferedMessagesCallback processedCallback) {
        for (Message<?> message : messagesUpForProcessing) {
            outputChannel.send(message);
        }
    }
}
